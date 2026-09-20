package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.nanitabeta.backend.data.Shop;

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
    List<Shop> actual = sut.searchShopSuggestions(20L, null, 20);

    assertThat(actual).extracting(Shop::getShopName)
        .containsExactly("スターバックス", "セブンイレブン");
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の候補_キーワードで部分一致の絞り込みができること() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "セブン", 20);

    assertThat(actual).extracting(Shop::getId).containsExactly(2L);
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の候補_キーワードは濁点と大文字小文字を区別しないこと() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "スターパックス", 20);

    assertThat(actual).extracting(Shop::getId).containsExactly(1L);
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の候補_該当がない場合は空のリストを返すこと() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "ローソン", 20);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-shops-for-suggestions.sql")
  void 店の候補_件数の上限を超えないこと() {
    List<Shop> actual = sut.searchShopSuggestions(20L, null, 1);

    assertThat(actual).hasSize(1);
  }

  @Test
  @Sql("/sql/insert-shops-with-wildcards.sql")
  void 店の候補_エスケープ済みのキーワードはワイルドカードとして扱われないこと() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "100\\%\\_OFF", 10);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("100%_OFF");
  }

  @Test
  @Sql("/sql/insert-shops-with-wildcards.sql")
  void 店の候補_エスケープしないキーワードはワイルドカードとして扱われること() {
    List<Shop> actual = sut.searchShopSuggestions(20L, "100%_OFF", 10);

    assertThat(actual).extracting(Shop::getShopName).containsExactly("100%_OFF", "100XYOFF");
  }
}
