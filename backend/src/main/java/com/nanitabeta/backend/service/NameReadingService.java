package com.nanitabeta.backend.service;

import com.nanitabeta.backend.util.NameNormalizer;
import com.worksap.nlp.sudachi.Config;
import com.worksap.nlp.sudachi.Dictionary;
import com.worksap.nlp.sudachi.Morpheme;
import com.worksap.nlp.sudachi.Tokenizer;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 店名・商品名から、候補検索に使う読みを作るサービスです。
 * <p>
 * 形態素解析の Sudachi（辞書は core）で名前を言葉に分け、それぞれの読み（カタカナ）をつなげます。 読みは候補検索だけに使い、同じ店・商品かどうかの判定には使いません。
 * 辞書の読み間違い（例：鶏 → ニワトリ）はそのまま受け入れ、元の名前での検索は今までどおり残します。
 */
@Service
public class NameReadingService implements AutoCloseable {

  /** 読まずに残す数字の形（半角・全角の数字。桁区切りのカンマと小数点も入れる。例：100、１，０００、1.5） */
  private static final Pattern NUMBER = Pattern.compile("[0-9０-９]+([,，.．][0-9０-９]+)*");
  /** 英字のかたまりの形（半角の英字。単語の間の空白も含める。例：抹茶Latte の Latte、cafe latte 全体） */
  private static final Pattern ALPHABET_PART = Pattern.compile("[A-Za-z]+( +[A-Za-z]+)*");

  /** 起動時に 1 回だけ開き、読みを作るたびに使い回す辞書（約 200MB のファイル） */
  private final Dictionary dictionary;

  /**
   * 辞書を開きます。
   * <p>
   * 辞書のファイルがない・読めない場合は例外になり、アプリが起動しません （読みを作れないまま動き出すより、起動時に気づけるようにするため）。
   *
   * @param dictionaryPath 辞書のファイルの場所（{@code application.yaml} の {@code sudachi.dictionary-path}）
   * @throws IOException 辞書のファイルを読めなかった場合
   */
  public NameReadingService(@Value("${sudachi.dictionary-path}") String dictionaryPath)
      throws IOException {
    // Sudachi の決まりの設定のうち、辞書のファイルだけを build.gradle で用意したものに差しかえる
    Config config = Config.defaultConfig().systemDictionary(Path.of(dictionaryPath));
    this.dictionary = Dictionary.load(config);
  }

  /**
   * 名前の読みを作ります。
   * <p>
   * 例：「抹茶ラテ」→「マッチャラテ」、「Latte」→「ラテ」、「1,000円」→「1,000エン」、「ラテ（ホット）」→「ラテ（ホット）」。
   *
   * @param name 店名・商品名
   * @return 読み（カタカナ。数字・記号・辞書にない言葉は元の文字のまま）
   */
  public String createReading(String name) {
    StringBuilder reading = new StringBuilder();
    // 言葉に分ける道具は同時に使うと壊れることがあるので、呼ばれるたびに作る（作るのは軽い）
    // SplitMode.C：いちばん長い区切りで分ける（「牛丼」を「牛」「丼」に分けず、ギュウドンと読む）
    for (Morpheme morpheme : dictionary.tokenizer().tokenize(Tokenizer.SplitMode.C, name)) {
      reading.append(readingOf(morpheme));
    }
    return reading.toString();
  }

  /**
   * 1 つの言葉の読みを決めます。
   * <p>
   * 上から順に調べ、最初に当てはまったものを返します。
   *
   * @param morpheme 名前を分けた 1 つの言葉
   * @return 読み
   */
  private String readingOf(Morpheme morpheme) {
    String surface = morpheme.surface();
    // 空白は読みに入れない（Sudachi は空白を「キゴウ」と読むため）
    if (surface.isBlank()) {
      return "";
    }
    // 記号は元の文字を残す（「（」「〜」なども「キゴウ」と読まれるため。「・」「!」はもともと元の文字）
    if (morpheme.partOfSpeech().get(0).equals("補助記号")) {
      return surface;
    }
    // 数字は読まずに半角で残す（「１，０００」を「イチ,レイレイレイ」ではなく「1,000」にする）
    if (NUMBER.matcher(surface).matches()) {
      return Normalizer.normalize(surface, Normalizer.Form.NFKC);
    }
    // 辞書にない言葉（未知語）は、Sudachi が仮に作った読み（例：QzxvABC → qzxvabc）を使わず、元の文字を残す
    if (morpheme.isOOV() || morpheme.readingForm().isEmpty()) {
      return surface;
    }
    return morpheme.readingForm();
  }

  /**
   * 候補検索の入力から、名前や名前の読みと比べる形を作ります。
   * <p>
   * 入力をそろえたもの（ひらがなはカタカナに）を必ず入れます。 英字を含む入力は、英字の部分だけを読みにしてそろえたものも入れます
   * （「latte」で「ラテ」、「抹茶Latte」で「抹茶ラテ」を見つけるため）。 漢字やかなは読みに変えません（「抹」を「マツ」と読んで、松屋が混ざるのを防ぐため）。 同じ形は 1
   * つにまとめ、空の形は入れません（空の形はどの名前にも含まれてしまうため）。
   *
   * @param keyword 候補検索の入力
   * @return 比べる形の一覧（比べる形がない場合は空のリスト）
   */
  public List<String> createKeywordForms(String keyword) {
    String normalized =
        NameNormalizer.normalize(Normalizer.normalize(keyword, Normalizer.Form.NFKC));
    Set<String> forms = new LinkedHashSet<>();
    forms.add(toComparable(normalized));
    if (ALPHABET_PART.matcher(normalized).find()) {
      forms.add(toComparable(readAlphabetParts(normalized)));
    }
    forms.remove("");
    return List.copyOf(forms);
  }

  /**
   * 英字のかたまりだけを読みにし、ほかの文字はそのまま残します。
   * <p>
   * 例：「抹茶Latte」→「抹茶ラテ」、「latte2」→「ラテ2」、「cafe latte」→ 全体を 1 つのかたまりとして読む。
   *
   * @param text そろえた入力（NFKC で英字を半角にしたもの）
   * @return 英字の部分だけを読みにした文字
   */
  private String readAlphabetParts(String text) {
    Matcher matcher = ALPHABET_PART.matcher(text);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      // 読みに $ や \ があっても、置きかえの特別な記号として扱われないようにする
      matcher.appendReplacement(result, Matcher.quoteReplacement(createReading(matcher.group())));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /**
   * 名前か名前の読みが、比べる形のどれかを含むかを調べます。
   * <p>
   * 名前とも比べるのは、英字の部分だけを読みにした形（例：抹茶ラテ）には漢字が残っていて、 カタカナだけの読み（マッチャラテ）には含まれないためです。
   *
   * @param name 店名・商品名
   * @param keywordForms {@link #createKeywordForms} で作った比べる形
   * @return 含む場合は true
   */
  public boolean matchesReading(String name, List<String> keywordForms) {
    String comparableName = toComparable(name);
    String reading = toComparable(createReading(name));
    for (String keywordForm : keywordForms) {
      if (comparableName.contains(keywordForm) || reading.contains(keywordForm)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 名前・読みと入力を比べられるよう、同じ形にそろえます（候補検索の比べにだけ使う。同じ名前かの判定には使わない）。
   * <p>
   * 全角半角をそろえ（NFKC）、小文字にし、ひらがなをカタカナにして、空白を取り除きます。 例：「まっちゃ
   * らて」→「マッチャラテ」、「（ホット）」→「(ホット)」、「QzxvABC」→「qzxvabc」。
   *
   * @param text 名前・読み・入力のどれか
   * @return そろえた文字
   */
  private String toComparable(String text) {
    String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    StringBuilder comparable = new StringBuilder();
    for (char c : normalized.toCharArray()) {
      if (Character.isWhitespace(c)) {
        continue;
      }
      // ひらがな（ぁ〜ゖ）だけを、同じ並びのカタカナ（ァ〜ヶ）にずらす。記号などは変えない
      if (c >= 'ぁ' && c <= 'ゖ') {
        comparable.append((char) (c + 0x60));
      } else {
        comparable.append(c);
      }
    }
    return comparable.toString();
  }

  /**
   * 辞書のファイルを閉じます。アプリを止めるときに Spring が呼びます。
   *
   * @throws IOException 辞書を閉じられなかった場合
   */
  @Override
  public void close() throws IOException {
    dictionary.close();
  }
}
