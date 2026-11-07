package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 名前の読みのテストです。
 * <p>
 * build.gradle の prepareSudachiDictionary が用意した本物の辞書（core）を使います（test タスクの前に用意される）。
 * 辞書を開くのは重いので、テストのクラスで 1 回だけ開きます。
 */
class NameReadingServiceTest {

  private static final String DICTIONARY_PATH =
      "build/sudachi/sudachi-dictionary-20261015/system_core.dic";

  private static NameReadingService sut;

  @BeforeAll
  static void openDictionary() throws IOException {
    sut = new NameReadingService(DICTIONARY_PATH);
  }

  @AfterAll
  static void closeDictionary() throws IOException {
    sut.close();
  }

  @Test
  void 漢字とカタカナの名前をカタカナの読みにすること() {
    String actual = sut.createReading("抹茶ラテ");

    assertThat(actual).isEqualTo("マッチャラテ");
  }

  @Test
  void ひらがなの名前もカタカナの読みにすること() {
    String actual = sut.createReading("からあげクン");

    assertThat(actual).isEqualTo("カラアゲクン");
  }

  @Test
  void 食べ物の言葉は1語として読むこと() {
    // Lucene Kuromoji では「ウシドンブリ」「トウアゲ」と読み間違えたもの
    assertThat(sut.createReading("牛丼")).isEqualTo("ギュウドン");
    assertThat(sut.createReading("親子丼")).isEqualTo("オヤコドン");
    assertThat(sut.createReading("唐揚げ")).isEqualTo("カラアゲ");
  }

  @Test
  void 辞書にある店名は店名の読みにすること() {
    // Lucene Kuromoji では「オオドヤ」と読み間違えたもの
    assertThat(sut.createReading("大戸屋")).isEqualTo("オオトヤ");
    assertThat(sut.createReading("吉野家")).isEqualTo("ヨシノヤ");
  }

  @Test
  void 英字の名前も辞書にあればカタカナの読みにすること() {
    assertThat(sut.createReading("Latte")).isEqualTo("ラテ");
  }

  @Test
  void 全角の英字と半角のカタカナも同じ読みにすること() {
    assertThat(sut.createReading("Ｌａｔｔｅ")).isEqualTo("ラテ");
    assertThat(sut.createReading("ｶﾌｪﾗﾃ")).isEqualTo("カフェラテ");
  }

  @Test
  void 数字は読まずに半角の数字で残すこと() {
    String expected = "100エンコーヒー";

    assertThat(sut.createReading("１００円コーヒー")).isEqualTo(expected);
    assertThat(sut.createReading("100円コーヒー")).isEqualTo(expected);
  }

  @Test
  void 空白は読みに入れないこと() {
    // Sudachi は空白を「キゴウ」と読むので、そのままだと「マッチャキゴウラテ」になる
    assertThat(sut.createReading("抹茶 ラテ")).isEqualTo("マッチャラテ");
    assertThat(sut.createReading("期間限定　さくらラテ")).isEqualTo("キカンゲンテイサクララテ");
  }

  @Test
  void 店名と珈琲のような言葉を辞書の読みにすること() {
    // コメダ・珈琲・店は、どれも辞書にある言葉（未知語ではない）
    assertThat(sut.createReading("コメダ珈琲店")).isEqualTo("コメダコーヒーテン");
  }

  @Test
  void 辞書にない言葉は元の文字を大文字小文字のまま残すこと() {
    // Sudachi は未知語に仮の読み（qzxvabc）を付けるが、それを使わない
    assertThat(sut.createReading("QzxvABC")).isEqualTo("QzxvABC");
  }

  @Test
  void 辞書にない言葉と辞書にある言葉が混ざっても未知語の部分だけ元の文字を残すこと() {
    assertThat(sut.createReading("QzxvABC抹茶")).isEqualTo("QzxvABCマッチャ");
  }

  @Test
  void 桁区切りのある数字は読まずに半角で残すこと() {
    String expected = "1,000エン";

    assertThat(sut.createReading("1,000円")).isEqualTo(expected);
    assertThat(sut.createReading("１，０００円")).isEqualTo(expected);
  }

  @Test
  void 小数は読まずに半角で残すこと() {
    String expected = "1.5バイ";

    assertThat(sut.createReading("1.5倍")).isEqualTo(expected);
    assertThat(sut.createReading("１．５倍")).isEqualTo(expected);
  }

  @Test
  void 中黒は元の文字のまま残すこと() {
    assertThat(sut.createReading("ザ・チーズバーガー")).isEqualTo("ザ・チーズバーガー");
  }

  @Test
  void キゴウと読まれるかっこや波線も元の文字のまま残すこと() {
    assertThat(sut.createReading("ラテ（ホット）")).isEqualTo("ラテ（ホット）");
    assertThat(sut.createReading("〜ラテ〜")).isEqualTo("〜ラテ〜");
  }

  @Test
  void 鶏はニワトリと読むこと_受け入れている読み間違い() {
    // 本当はトリノカラアゲベントウ。辞書の読みを受け入れると決めたもの（変わったら気づけるように固定する）
    assertThat(sut.createReading("鶏の唐揚げ弁当")).isEqualTo("ニワトリノカラアゲベントウ");
  }

  @Test
  void 複数のスレッドから同時に読みを作っても正しい読みになること() throws Exception {
    List<String> names = List.of("抹茶ラテ", "牛丼", "大戸屋", "Latte", "1,000円");
    List<String> expected = List.of("マッチャラテ", "ギュウドン", "オオトヤ", "ラテ", "1,000エン");

    try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
      List<Future<List<String>>> futures = new ArrayList<>();
      for (int i = 0; i < 40; i++) {
        futures.add(executor.submit(() -> names.stream().map(sut::createReading).toList()));
      }
      for (Future<List<String>> future : futures) {
        List<String> actual = future.get();

        assertThat(actual).isEqualTo(expected);
      }
    }
  }

  @Test
  void 比べる形_ひらがなとカタカナと半角カナの入力は同じカタカナの形になること() {
    List<String> expected = List.of("マッチャ");

    assertThat(sut.createKeywordForms("まっちゃ")).isEqualTo(expected);
    assertThat(sut.createKeywordForms("マッチャ")).isEqualTo(expected);
    assertThat(sut.createKeywordForms("ﾏｯﾁｬ")).isEqualTo(expected);
  }

  @Test
  void 比べる形_空白は取り除くこと() {
    assertThat(sut.createKeywordForms("まっちゃ らて")).isEqualTo(List.of("マッチャラテ"));
    assertThat(sut.createKeywordForms("まっちゃ　らて")).isEqualTo(List.of("マッチャラテ"));
  }

  @Test
  void 比べる形_分かれた濁点は1文字にまとめること() {
    // 「か」＋濁点の記号（結合文字）→「が」
    assertThat(sut.createKeywordForms("が")).isEqualTo(List.of("ガ"));
  }

  @Test
  void 比べる形_英字だけの入力は読みの形も入れること() {
    List<String> expected = List.of("latte", "ラテ");

    assertThat(sut.createKeywordForms("latte")).isEqualTo(expected);
    assertThat(sut.createKeywordForms("Latte")).isEqualTo(expected);
    assertThat(sut.createKeywordForms("LATTE")).isEqualTo(expected);
    assertThat(sut.createKeywordForms("Ｌａｔｔｅ")).isEqualTo(expected);
  }

  @Test
  void 比べる形_辞書にない英字の途中の入力は1つにまとまること() {
    // lat は辞書にないので読みも lat のまま。同じ形は 1 つにまとめる
    assertThat(sut.createKeywordForms("lat")).isEqualTo(List.of("lat"));
  }

  @Test
  void 比べる形_漢字の入力は読みに変えないこと() {
    // 「抹」を「マツ」にすると松屋（マツヤ）が混ざるため
    assertThat(sut.createKeywordForms("抹")).isEqualTo(List.of("抹"));
  }

  @Test
  void 比べる形_英字以外が混ざった入力は英字の部分だけ読みにした形も入れること() {
    assertThat(sut.createKeywordForms("抹茶latte")).isEqualTo(List.of("抹茶latte", "抹茶ラテ"));
    assertThat(sut.createKeywordForms("抹茶Ｌａｔｔｅ")).isEqualTo(List.of("抹茶latte", "抹茶ラテ"));
    assertThat(sut.createKeywordForms("latte2")).isEqualTo(List.of("latte2", "ラテ2"));
  }

  @Test
  void 比べる形_空白をはさんだ英字の句は1つのかたまりとして読みにすること() {
    // 「cafe」「latte」を別々に読まず、「cafe latte」を 1 つのかたまりとして読む。比べる形では空白を取り除く
    List<String> expected = List.of("cafelatte", "カフェラテ");

    assertThat(sut.createKeywordForms("cafe latte")).isEqualTo(expected);
    assertThat(sut.createKeywordForms("Cafe　Latte")).isEqualTo(expected); // 全角スペース
  }

  @Test
  void 比べる形_英字の部分が離れて複数ある入力はどちらも読みにすること() {
    assertThat(sut.createKeywordForms("latte抹茶mocha")).isEqualTo(List.of("latte抹茶mocha", "ラテ抹茶モカ"));
    assertThat(sut.createKeywordForms("Latteとmocha")).isEqualTo(List.of("latteトmocha", "ラテトモカ"));
  }

  @Test
  void 比べる形_空や空白だけの入力は空のリストになること() {
    assertThat(sut.createKeywordForms("")).isEmpty();
    assertThat(sut.createKeywordForms(" 　 ")).isEmpty();
  }

  @Test
  void 比べる形_LIKEで特別な記号も普通の文字として残すこと() {
    assertThat(sut.createKeywordForms("100%_\\")).isEqualTo(List.of("100%_\\"));
  }

  @Test
  void 読みの一致_ひらがなの入力で漢字の名前に当たること() {
    List<String> keywordForms = sut.createKeywordForms("まっちゃ");

    assertThat(sut.matchesReading("抹茶ラテ", keywordForms)).isTrue();
    assertThat(sut.matchesReading("ほうじ茶ラテ", keywordForms)).isFalse();
  }

  @Test
  void 読みの一致_食べ物と店名の読みに当たること() {
    assertThat(sut.matchesReading("牛丼", sut.createKeywordForms("ぎゅうどん"))).isTrue();
    assertThat(sut.matchesReading("大戸屋", sut.createKeywordForms("おおとや"))).isTrue();
  }

  @Test
  void 読みの一致_かなの入力で英字の名前に当たること() {
    assertThat(sut.matchesReading("Latte", sut.createKeywordForms("らて"))).isTrue();
  }

  @Test
  void 読みの一致_英字の入力でカタカナの名前に当たること() {
    List<String> keywordForms = sut.createKeywordForms("latte");

    assertThat(sut.matchesReading("ラテ", keywordForms)).isTrue();
    assertThat(sut.matchesReading("抹茶ラテ", keywordForms)).isTrue();
  }

  @Test
  void 読みの一致_漢字と英字が混ざった入力で英字をカタカナにした名前に当たること() {
    List<String> keywordForms = sut.createKeywordForms("抹茶Latte");

    assertThat(sut.matchesReading("抹茶ラテ", keywordForms)).isTrue();
    assertThat(sut.matchesReading("抹茶ラテ（ホット）", keywordForms)).isTrue();
    assertThat(sut.matchesReading("ほうじ茶ラテ", keywordForms)).isFalse();
  }

  @Test
  void 読みの一致_空白をはさんだ英字の句でカタカナや英字の名前に当たること() {
    List<String> keywordForms = sut.createKeywordForms("cafe latte");

    assertThat(sut.matchesReading("カフェラテ", keywordForms)).isTrue();
    assertThat(sut.matchesReading("Cafe Latte", keywordForms)).isTrue();
    assertThat(sut.matchesReading("ラテ", keywordForms)).isFalse();
  }

  @Test
  void 読みの一致_英字の部分が複数ある入力で英字をどちらもカタカナにした名前に当たること() {
    List<String> keywordForms = sut.createKeywordForms("latte抹茶mocha");

    assertThat(sut.matchesReading("ラテ抹茶モカ", keywordForms)).isTrue();
    assertThat(sut.matchesReading("ラテ抹茶", keywordForms)).isFalse();
  }

  @Test
  void 読みの一致_漢字と英字が混ざった入力ではカタカナだけの名前に当たらないこと() {
    // 漢字は読まないと決めているため（受け入れている取りこぼし。変わったら気づけるように固定する）
    assertThat(sut.matchesReading("マッチャラテ", sut.createKeywordForms("抹茶Latte"))).isFalse();
  }

  @Test
  void 読みの一致_漢字の入力では読みが同じでも違う漢字の名前に当たらないこと() {
    assertThat(sut.matchesReading("松屋", sut.createKeywordForms("抹"))).isFalse();
  }

  @Test
  void 読みの一致_空白の入り方が違っても当たること() {
    assertThat(sut.matchesReading("抹茶ラテ", sut.createKeywordForms("まっちゃ らて"))).isTrue();
    assertThat(sut.matchesReading("抹茶 ラテ", sut.createKeywordForms("まっちゃらて"))).isTrue();
  }

  @Test
  void 読みの一致_全角と半角の記号の違いがあっても当たること() {
    // 名前の読みは「ラテ（ホット）」（全角のかっこ）、入力は半角のかっこ。両方をそろえて比べる
    // （「抹茶（ホット）」は辞書に 1 語で入っていて読みが「マッチャ」になるので、例に使わない）
    assertThat(sut.matchesReading("ラテ（ホット）", sut.createKeywordForms("らて(ほっと)"))).isTrue();
  }

  @Test
  void 読みの一致_辞書にない英字は大文字小文字を区別しないこと() {
    assertThat(sut.matchesReading("QzxvABC", sut.createKeywordForms("qzxv"))).isTrue();
  }

  @Test
  void 読みの一致_LIKEで特別な記号はワイルドカードにならないこと() {
    // Java の contains では % や _ は普通の文字（SQL の LIKE のように「何でもよい文字」にならない）
    assertThat(sut.matchesReading("1000円", sut.createKeywordForms("100%"))).isFalse();
    assertThat(sut.matchesReading("ラテ", sut.createKeywordForms("ら_"))).isFalse();
  }

  @Test
  void 読みの一致_比べる形がない場合は当たらないこと() {
    assertThat(sut.matchesReading("抹茶ラテ", List.of())).isFalse();
  }

  @Test
  void 辞書のファイルがない場合はファイルがない例外が発生すること() {
    assertThatThrownBy(() -> new NameReadingService("build/sudachi/not-found.dic"))
        .isInstanceOf(NoSuchFileException.class);
  }
}
