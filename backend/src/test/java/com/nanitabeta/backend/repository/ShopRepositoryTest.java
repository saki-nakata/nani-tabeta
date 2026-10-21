package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nanitabeta.backend.data.Shop;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShopRepositoryTest {

  @Autowired
  private ShopRepository sut;

  @Test
  @Sql("/sql/insert-shop.sql")
  void 店を1件取得できること() {
    Shop actual = sut.searchShop(1L);

    assertThat(actual.getShopName()).isEqualTo("スターバックス");
    assertThat(actual.getAreaId()).isEqualTo(20L);
    assertThat(actual.getShopKindId()).isEqualTo(9L);
    assertThat(actual.getIsClosed()).isFalse();
  }

  @Test
  void 店が見つからない場合はnullを返すこと() {
    Shop actual = sut.searchShop(999L);

    assertThat(actual).isNull();
  }

  @Test
  @Sql("/sql/insert-shop.sql")
  void 店名とエリアで店を取得できること() {
    Shop shop = new Shop(null, "スターバックス", 20L, null, null);

    Shop actual = sut.searchShopByNameAndArea(shop);

    assertThat(actual.getId()).isEqualTo(1L);
  }

  @Test
  @Sql("/sql/insert-shop.sql")
  void 同じ店名でもエリアが異なれば見つからないこと() {
    Shop shop = new Shop(null, "スターバックス", 13L, null, null);

    Shop actual = sut.searchShopByNameAndArea(shop);

    assertThat(actual).isNull();
  }

  @Test
  @Sql("/sql/insert-shop.sql")
  void 濁点が異なる店名は別の店として扱うこと() {
    Shop shop = new Shop(null, "スターパックス", 20L, null, null);

    Shop actual = sut.searchShopByNameAndArea(shop);

    assertThat(actual).isNull();
  }

  @Test
  void 店を登録できること() {
    Shop shop = new Shop(null, "セブンイレブン", 27L, 1L, null);

    sut.insertShop(shop);

    assertThat(shop.getId()).isNotNull();

    Shop actual = sut.searchShop(shop.getId());
    assertThat(actual.getShopName()).isEqualTo("セブンイレブン");
    assertThat(actual.getAreaId()).isEqualTo(27L);
    assertThat(actual.getShopKindId()).isEqualTo(1L);
    assertThat(actual.getIsClosed()).isFalse();
  }

  @Test
  @Sql("/sql/insert-shop.sql")
  void 店名とエリアで店をロックして取得できること() {
    Shop shop = new Shop(null, "スターバックス", 20L, null, null);

    Shop actual = sut.searchShopByNameAndAreaForShare(shop);

    assertThat(actual.getId()).isEqualTo(1L);
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の候補_エリア内の閉店していない店を店名順で取得できること() {
    List<Shop> actual = sut.searchShopSuggestions(20L, null, null, 20);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("スターバックス", "セブンイレブン");
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の候補_キーワードで部分一致の絞り込みができること() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "セブン", "セブン", 20);

    assertThat(actual).extracting(Shop::getId).containsExactly(2L);
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の候補_キーワードは濁点と大文字小文字を区別しないこと() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "スターパックス", "スターパックス", 20);

    assertThat(actual).extracting(Shop::getId).containsExactly(1L);
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の候補_該当がない場合は空のリストを返すこと() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "ローソン", "ローソン", 20);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の候補_件数の上限を超えないこと() {
    List<Shop> actual = sut.searchShopSuggestions(20L, null, null, 1);

    assertThat(actual).hasSize(1);
  }

  @Test
  @Sql("/sql/insert-shops-for-exact-match.sql")
  void 店の候補_完全一致を指定しない場合は名前順の上限までで完全一致の店が漏れること() {
    // テストデータが、直す前の不具合（名前順では完全一致が 11 件目以降）を再現できていることの確認
    List<Shop> actual = sut.searchShopSuggestions(20L, "Latte", null, 10);

    assertThat(actual).extracting(Shop::getShopName).hasSize(10).doesNotContain("Latte");
  }

  @Test
  @Sql("/sql/insert-shops-for-exact-match.sql")
  void 店の候補_完全に同じ店名は名前順で上限から漏れる場合でも先頭に返すこと() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "Latte", "Latte", 10);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("Latte", "A01 Latte",
        "A02 Latte", "A03 Latte", "A04 Latte", "A05 Latte", "A06 Latte", "A07 Latte", "A08 Latte",
        "A09 Latte");
  }

  @Test
  @Sql("/sql/insert-shops-for-exact-match.sql")
  void 店の候補_大文字小文字が違う店は完全一致にならず完全一致の後ろに名前順で残ること() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "Latte", "Latte", 20);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("Latte", "A01 Latte",
        "A02 Latte", "A03 Latte", "A04 Latte", "A05 Latte", "A06 Latte", "A07 Latte", "A08 Latte",
        "A09 Latte", "A10 Latte", "latte");
  }

  @Test
  @Sql("/sql/insert-shops-for-exact-match.sql")
  void 店の候補_ワイルドカードとバックスラッシュを含む店名でも完全一致を先頭に返すこと() {
    // 部分一致用はエスケープ済み（100\%\_\\OFF）、完全一致用はエスケープ前（100%_\OFF）
    List<Shop> actual = sut.searchShopSuggestions(21L, "100\\%\\_\\\\OFF", "100%_\\OFF", 10);

    assertThat(actual).hasSize(10);
    assertThat(actual.get(0).getShopName()).isEqualTo("100%_\\OFF");
  }

  @Test
  @Sql("/sql/insert-shops-for-exact-match.sql")
  void 店の候補_ひらがなとカタカナだけが違う店名は完全一致として先頭に返すこと() {
    // 店名の列の照合順序（utf8mb4_ja_0900_as_cs）では「らて」と「ラテ」は同じ
    List<Shop> actual = sut.searchShopSuggestions(22L, "らて", "らて", 10);

    assertThat(actual).hasSize(10);
    assertThat(actual.get(0).getShopName()).isEqualTo("ラテ");
  }

  @Test
  @Sql("/sql/insert-shops-for-exact-match.sql")
  void 店の候補_濁点が違う店は候補に残るが完全一致の後ろになること() {
    // 名前順では「からあけ」が先だが、完全一致の「からあげ」を先頭に並べる
    List<Shop> actual = sut.searchShopSuggestions(23L, "からあげ", "からあげ", 10);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("からあげ", "からあけ");
  }

  @Test
  @Sql("/sql/insert-shops-for-exact-match.sql")
  void 店の候補_閉店した店は完全一致でも候補に出ないこと() {
    List<Shop> actual = sut.searchShopSuggestions(24L, "Latte", "Latte", 10);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("A Latte");
  }

  @Test
  @Sql("/sql/insert-shops-for-exact-match.sql")
  void 店の候補_部分一致では見つからないが完全一致になる店も候補に含めること() {
    // 「カ」＋結合濁点（2文字）は、= では「ガ」（1文字）と一致するが、LIKE は1文字ずつ比べるので一致しない
    String keyword = "ガ";

    List<Shop> actual = sut.searchShopSuggestions(25L, keyword, keyword, 10);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("ガ");
  }

  @Test
  @Sql("/sql/insert-shops-for-exact-match.sql")
  void 店の候補_完全一致で集めるときもエリアと閉店の条件を守ること() {
    // エリア26の「ガ」は閉店。完全一致の条件が、エリア・閉店の条件の外側にかかると、
    // 閉店した「ガ」や、ほかのエリア（25）の「ガ」まで出てしまう
    String keyword = "ガ";

    List<Shop> actual = sut.searchShopSuggestions(26L, keyword, keyword, 10);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-shop.sql")
  void 店を更新できること() {
    Shop shop = new Shop(1L, "スタバ", 13L, 1L, true);

    sut.updateShop(shop);

    Shop actual = sut.searchShop(1L);
    assertThat(actual.getShopName()).isEqualTo("スタバ");
    assertThat(actual.getAreaId()).isEqualTo(13L);
    assertThat(actual.getShopKindId()).isEqualTo(1L);
    assertThat(actual.getIsClosed()).isTrue();
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の更新_同じ店名とエリアの店がすでにある場合は例外が発生すること() {
    Shop shop = new Shop(1L, "セブンイレブン", 20L, 9L, false);

    assertThatThrownBy(() -> sut.updateShop(shop)).isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  @Sql("/sql/insert-shop.sql")
  void 店の更新_店名を変えない場合は自分自身と重複しないこと() {
    Shop shop = new Shop(1L, "スターバックス", 20L, 1L, false);

    sut.updateShop(shop);

    assertThat(sut.searchShop(1L).getShopKindId()).isEqualTo(1L);
  }

  @Test
  @Sql("/sql/insert-shops-with-wildcards.sql")
  void 店の候補_エスケープ済みのキーワードはワイルドカードとして扱われないこと() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "100\\%\\_OFF", "100%_OFF", 10);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("100%_OFF");
  }

  @Test
  @Sql("/sql/insert-shops-with-wildcards.sql")
  void 店の候補_エスケープしないキーワードはワイルドカードとして扱われること() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "100%_OFF", null, 10);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("100%_OFF", "100XYOFF");
  }
}
