package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nanitabeta.backend.data.Item;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryTest {

  // よく使う順の候補で、記録を数える利用者（テストデータの Saki）
  private static final String MY_USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";

  @Autowired
  private ItemRepository sut;

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品を1件取得できること() {
    Item actual = sut.searchItem(1L);

    assertThat(actual.getShopId()).isEqualTo(1L);
    assertThat(actual.getItemName()).isEqualTo("からあげ棒");
    assertThat(actual.getCategoryId()).isEqualTo(4L);
    assertThat(actual.getIsSeasonal()).isFalse();
  }

  @Test
  void 商品が見つからない場合はnullを返すこと() {
    Item actual = sut.searchItem(999L);

    assertThat(actual).isNull();
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 商品の候補_店の商品を商品名順に取得できること() {
    List<Item> actual = sut.searchItemSuggestions(1L, null, null, 10);

    assertThat(actual).extracting(Item::getItemName).containsExactly("おにぎり 鮭", "からあげ棒");
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 商品の候補_商品が登録されていない店の場合は空のリストを返すこと() {
    List<Item> actual = sut.searchItemSuggestions(3L, null, null, 10);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 商品の候補_キーワードで部分一致の絞り込みができること() {
    List<Item> actual = sut.searchItemSuggestions(1L, "から", "から", 10);

    assertThat(actual).extracting(Item::getId).containsExactly(1L);
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 商品の候補_キーワードは濁点と大文字小文字を区別しないこと() {
    List<Item> actual = sut.searchItemSuggestions(1L, "からあけ棒", "からあけ棒", 10);

    assertThat(actual).extracting(Item::getId).containsExactly(1L);
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 候補になりうる商品_店の記録がある商品だけを商品名順に取得できること() {
    // 店 1 の商品は からあげ棒（記録あり）・おにぎり 鮭（記録あり）・からあげ串（記録なし）。店 2 の からあげ棒 は入らない
    List<Item> actual = sut.searchAllItemSuggestions(1L);

    assertThat(actual).extracting(Item::getId).containsExactly(2L, 1L);
    assertThat(actual).extracting(Item::getItemName).containsExactly("おにぎり 鮭", "からあげ棒");
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 候補になりうる商品_今の候補と同じ列を返すこと() {
    List<Item> actual = sut.searchAllItemSuggestions(1L);

    Item expected = sut.searchItemSuggestions(1L, null, null, 10).get(0);
    assertThat(actual.get(0)).isEqualTo(expected);
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql",
      "/sql/insert-item-records-twice.sql"})
  void 候補になりうる商品_記録が複数ある商品も1件だけ返すこと() {
    List<Item> actual = sut.searchAllItemSuggestions(1L);

    assertThat(actual).extracting(Item::getId).containsExactly(2L, 1L);
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 候補になりうる商品_商品が登録されていない店の場合は空のリストを返すこと() {
    List<Item> actual = sut.searchAllItemSuggestions(3L);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 商品の候補_該当がない場合は空のリストを返すこと() {
    List<Item> actual = sut.searchItemSuggestions(1L, "ラーメン", "ラーメン", 10);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 商品の候補_件数の上限を超えないこと() {
    List<Item> actual = sut.searchItemSuggestions(1L, null, null, 1);

    assertThat(actual).hasSize(1);
  }

  @Test
  @Sql("/sql/insert-items-for-exact-match.sql")
  void 商品の候補_完全一致を指定しない場合は名前順の上限までで完全一致の商品が漏れること() {
    // テストデータが、直す前の不具合（名前順では完全一致が 11 件目以降）を再現できていることの確認
    List<Item> actual = sut.searchItemSuggestions(1L, "Latte", null, 10);

    assertThat(actual).extracting(Item::getItemName).hasSize(10).doesNotContain("Latte");
  }

  @Test
  @Sql("/sql/insert-items-for-exact-match.sql")
  void 商品の候補_完全に同じ商品名は名前順で上限から漏れる場合でも先頭に返すこと() {
    List<Item> actual = sut.searchItemSuggestions(1L, "Latte", "Latte", 10);

    assertThat(actual).extracting(Item::getItemName).containsExactly("Latte", "A01 Latte",
        "A02 Latte", "A03 Latte", "A04 Latte", "A05 Latte", "A06 Latte", "A07 Latte", "A08 Latte",
        "A09 Latte");
  }

  @Test
  @Sql("/sql/insert-items-for-exact-match.sql")
  void 商品の候補_大文字小文字だけが違う商品名もLOWER一致として先頭に返すこと() {
    // 名前順では「Latte」は 11 件目。LOWER(商品名) どうしで一致する商品を先頭に並べる
    List<Item> actual = sut.searchItemSuggestions(1L, "latte", "latte", 10);

    assertThat(actual).extracting(Item::getItemName).containsExactly("Latte", "A01 Latte",
        "A02 Latte", "A03 Latte", "A04 Latte", "A05 Latte", "A06 Latte", "A07 Latte", "A08 Latte",
        "A09 Latte");
  }

  @Test
  @Sql("/sql/insert-items-for-exact-match.sql")
  void 商品の候補_ワイルドカードとバックスラッシュを含む商品名でも完全一致を先頭に返すこと() {
    // 部分一致用はエスケープ済み（100\%\_\\OFF）、完全一致用はエスケープ前（100%_\OFF）
    List<Item> actual = sut.searchItemSuggestions(2L, "100\\%\\_\\\\OFF", "100%_\\OFF", 10);

    assertThat(actual).hasSize(10);
    assertThat(actual.get(0).getItemName()).isEqualTo("100%_\\OFF");
  }

  @Test
  @Sql("/sql/insert-items-for-exact-match.sql")
  void 商品の候補_ひらがなとカタカナだけが違う商品名は完全一致として先頭に返すこと() {
    // 商品名の列の照合順序（utf8mb4_ja_0900_as_cs）では「らて」と「ラテ」は同じ
    List<Item> actual = sut.searchItemSuggestions(3L, "らて", "らて", 10);

    assertThat(actual).hasSize(10);
    assertThat(actual.get(0).getItemName()).isEqualTo("ラテ");
  }

  @Test
  @Sql("/sql/insert-items-for-exact-match.sql")
  void 商品の候補_部分一致では見つからないが完全一致になる商品も候補に含めること() {
    // 「カ」＋結合濁点（2文字）は、= では「ガ」（1文字）と一致するが、LIKE は1文字ずつ比べるので一致しない
    String keyword = "ガ";

    List<Item> actual = sut.searchItemSuggestions(5L, keyword, keyword, 10);

    assertThat(actual).extracting(Item::getItemName).containsExactly("ガ");
  }

  @Test
  @Sql("/sql/insert-items-for-exact-match.sql")
  void 商品の候補_濁点が違う商品は候補に残るが完全一致の後ろになること() {
    // 名前順では「からあけ」が先だが、完全一致の「からあげ」を先頭に並べる
    List<Item> actual = sut.searchItemSuggestions(4L, "からあげ", "からあげ", 10);

    assertThat(actual).extracting(Item::getItemName).containsExactly("からあげ", "からあけ");
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 店と商品名で商品を取得できること() {
    Item item = new Item(null, 1L, "からあげ棒", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual.getId()).isEqualTo(1L);
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 同じ商品名でも店が異なれば別の商品として取得されること() {
    Item item = new Item(null, 2L, "からあげ棒", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual.getId()).isEqualTo(3L);
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 濁点が異なる商品名は別の商品として扱うこと() {
    Item item = new Item(null, 1L, "からあけ棒", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual).isNull();
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 店と商品名で商品をロックして取得できること() {
    Item item = new Item(null, 1L, "からあげ棒", null, null);

    Item actual = sut.searchItemByShopAndNameForShare(item);

    assertThat(actual.getId()).isEqualTo(1L);
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 大文字小文字だけが違う商品名は同じ商品として取得されること() {
    Item item = new Item(null, 1L, "LATTE", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual.getId()).isEqualTo(1L);
    assertThat(actual.getItemName()).isEqualTo("Latte"); // 保存されている表記のまま
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 大文字小文字だけが違う商品名はロックして取得するときも同じ商品として取得されること() {
    Item item = new Item(null, 1L, "latte", null, null);

    Item actual = sut.searchItemByShopAndNameForShare(item);

    assertThat(actual.getId()).isEqualTo(1L);
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 大文字小文字が同じでも店が異なれば別の商品として扱うこと() {
    Item item = new Item(null, 2L, "latte", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual).isNull();
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 小さいかなと大きいかなが違う商品名は別の商品として扱うこと() {
    Item item = new Item(null, 1L, "きやべつ", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual).isNull();
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 半角カナと全角カナが違う商品名は別の商品として扱うこと() {
    Item item = new Item(null, 1L, "ﾗｰﾒﾝ", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual).isNull();
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 丸数字と数字が違う商品名は別の商品として扱うこと() {
    Item item = new Item(null, 1L, "①番", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual).isNull();
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void アクセントの有無が違う商品名は別の商品として扱うこと() {
    Item item = new Item(null, 1L, "Cafe", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual).isNull();
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void アクセント付きの英字の大文字小文字もMySQLのLOWERに従って同じ商品として扱うこと() {
    // 英字（ASCII）だけでなく、MySQL の LOWER() が小文字にする文字はすべて同じとみなす
    Item item = new Item(null, 1L, "CAFÉ", null, null);

    Item actual = sut.searchItemByShopAndName(item);

    assertThat(actual.getId()).isEqualTo(5L);
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品を登録できること() {
    Item item = new Item(null, 1L, "肉まん", 8L, true);

    sut.insertItem(item);

    assertThat(item.getId()).isNotNull();

    Item actual = sut.searchItem(item.getId());
    assertThat(actual.getShopId()).isEqualTo(1L);
    assertThat(actual.getItemName()).isEqualTo("肉まん");
    assertThat(actual.getCategoryId()).isEqualTo(8L);
    assertThat(actual.getIsSeasonal()).isTrue();
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 商品の登録_同じ店に大文字小文字だけが違う商品名の商品がすでにある場合は例外が発生すること() {
    Item item = new Item(null, 1L, "LATTE", 10L, false);

    assertThatThrownBy(() -> sut.insertItem(item)).isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 商品の登録_店が異なれば同じ商品名で登録できること() {
    Item item = new Item(null, 2L, "Latte", 10L, false);

    sut.insertItem(item);

    assertThat(sut.searchItem(item.getId()).getShopId()).isEqualTo(2L);
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品を更新できること() {
    Item item = new Item(1L, null, "からあげクン", 4L, true);

    sut.updateItem(item);

    Item actual = sut.searchItem(1L);
    assertThat(actual.getItemName()).isEqualTo("からあげクン");
    assertThat(actual.getCategoryId()).isEqualTo(4L);
    assertThat(actual.getIsSeasonal()).isTrue();
    assertThat(actual.getShopId()).isEqualTo(1L);
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品の更新_同じ店に同じ商品名の商品がすでにある場合は例外が発生すること() {
    Item item = new Item(1L, null, "おにぎり 鮭", 4L, false);

    assertThatThrownBy(() -> sut.updateItem(item)).isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品の更新_商品名を変えない場合は自分自身と重複しないこと() {
    Item item = new Item(1L, null, "からあげ棒", 8L, false);

    sut.updateItem(item);

    assertThat(sut.searchItem(1L).getCategoryId()).isEqualTo(8L);
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 商品の更新_ほかの商品と大文字小文字だけが違う商品名に変える場合は例外が発生すること() {
    // 店1の「Mocha」（id=6）を、同じ店の「Latte」の大文字小文字違いに変える
    Item item = new Item(6L, null, "latte", 10L, false);

    assertThatThrownBy(() -> sut.updateItem(item)).isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  @Sql("/sql/insert-items-for-letter-case.sql")
  void 商品の更新_自分自身の大文字小文字だけを変える場合は更新できること() {
    Item item = new Item(1L, null, "LATTE", 10L, true);

    sut.updateItem(item);

    assertThat(sut.searchItem(1L).getItemName()).isEqualTo("LATTE");
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 商品の候補_記録がない商品は候補に出ないこと() {
    // 店1の「からあげ串」（id=4）は記録が0件
    List<Item> actual = sut.searchItemSuggestions(1L, "から", "から", 10);

    assertThat(actual).extracting(Item::getId).containsExactly(1L);
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 記録がない商品は削除されること() {
    sut.deleteItemIfNoRecords(4L);

    assertThat(sut.searchItem(4L)).isNull();
  }

  @Test
  @Sql({"/sql/insert-items.sql", "/sql/insert-item-records.sql"})
  void 記録がある商品は削除されないこと() {
    Item expected = sut.searchItem(1L);

    sut.deleteItemIfNoRecords(1L);

    assertThat(sut.searchItem(1L)).isEqualTo(expected);
  }

  // ここから、キーワードがないときの商品の候補（よく使う順）。データの並びは insert-items-for-my-records.sql の先頭に書いてある

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_記録の件数_最後に食べた日_商品名の順に並び自分の記録がない商品はうしろに商品名順で並ぶこと() {
    List<Item> actual = sut.searchItemSuggestionsByMyRecords(1L, MY_USER_ID, 20);

    assertThat(actual).extracting(Item::getItemName).containsExactly("Z からあげクン", "X おにぎり", "Y プリン",
        "R サラダ", "S パン", "W 肉まん", "A01", "A02", "A03", "A04", "A05", "A06", "A07", "A08",
        "B ほかの人だけの商品");
  }

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_商品名順では上限から漏れるよく使う商品も先頭に返すこと() {
    // テストデータが、今までの候補（商品名順）では Z からあげクン が 10 件に入らない形になっていることの確認
    List<Item> nameOrder = sut.searchItemSuggestions(1L, null, null, 10);
    assertThat(nameOrder).extracting(Item::getItemName).doesNotContain("Z からあげクン");

    List<Item> actual = sut.searchItemSuggestionsByMyRecords(1L, MY_USER_ID, 10);

    assertThat(actual).extracting(Item::getItemName).containsExactly("Z からあげクン", "X おにぎり", "Y プリン",
        "R サラダ", "S パン", "W 肉まん", "A01", "A02", "A03", "A04");
  }

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_同じ件数なら登録した日時ではなく最後に食べた日が新しい商品を先にすること() {
    // X おにぎり は食べた日が新しく（11/01）登録した日時が古い。Y プリン は食べた日が古く（10/01）登録した日時が新しい
    List<Item> actual = sut.searchItemSuggestionsByMyRecords(1L, MY_USER_ID, 10);

    assertThat(actual).extracting(Item::getItemName).containsSubsequence("X おにぎり", "Y プリン");
  }

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_件数も最後に食べた日も同じなら商品名順に並ぶこと() {
    // R サラダ と S パン は、どちらも 1 件・09/01。記録を入れた順（S パン が先）ではなく商品名順
    List<Item> actual = sut.searchItemSuggestionsByMyRecords(1L, MY_USER_ID, 10);

    assertThat(actual).extracting(Item::getItemName).containsSubsequence("R サラダ", "S パン");
  }

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_ほかの人の記録は件数にも最後に食べた日にも使わないが候補には出すこと() {
    // W 肉まん は、ほかの人の新しい記録（11/20）を数えると 2 件・11/20 になり、先頭に来てしまう
    // B ほかの人だけの商品 は、自分の記録はないが、ほかの人の記録があるので候補に出て、最後に並ぶ
    List<Item> actual = sut.searchItemSuggestionsByMyRecords(1L, MY_USER_ID, 20);

    assertThat(actual).extracting(Item::getItemName).containsSubsequence("R サラダ", "S パン", "W 肉まん",
        "A01");
    assertThat(actual).last().extracting(Item::getItemName).isEqualTo("B ほかの人だけの商品");
  }

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_だれの記録もない商品は出さないこと() {
    List<Item> actual = sut.searchItemSuggestionsByMyRecords(1L, MY_USER_ID, 20);

    assertThat(actual).extracting(Item::getItemName).doesNotContain("N だれも記録していない商品");
  }

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_ほかの店の同じ名前の商品は出さず記録も数えないこと() {
    // 店2の「W 肉まん」は Saki の記録が 4 件。混ざると W 肉まん が先頭に来るか、2 件並ぶ
    List<Item> actual = sut.searchItemSuggestionsByMyRecords(1L, MY_USER_ID, 20);

    assertThat(actual).extracting(Item::getShopId).containsOnly(1L);
    assertThat(actual).filteredOn(item -> item.getItemName().equals("W 肉まん"))
        .extracting(Item::getId).containsExactly(4L);
    assertThat(actual).extracting(Item::getItemName).containsSubsequence("S パン", "W 肉まん");
  }

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_自分の記録がひとつもない場合は今までの候補と同じ商品名順になること() {
    // 記録を 1 件も持たない利用者
    String userId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";

    List<Item> actual = sut.searchItemSuggestionsByMyRecords(1L, userId, 10);

    List<Item> expected = sut.searchItemSuggestions(1L, null, null, 10);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_存在しない店IDの場合は空のリストを返すこと() {
    // 店99は存在しない（存在するかの確認は Service で行う）
    List<Item> actual = sut.searchItemSuggestionsByMyRecords(99L, MY_USER_ID, 10);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-items-for-my-records.sql")
  void よく使う順の商品の候補_商品はあるがだれの記録もない店の場合は空のリストを返すこと() {
    // 店3には商品が 1 つあるが、だれの記録もない
    List<Item> actual = sut.searchItemSuggestionsByMyRecords(3L, MY_USER_ID, 10);

    assertThat(actual).isEmpty();
  }
}
