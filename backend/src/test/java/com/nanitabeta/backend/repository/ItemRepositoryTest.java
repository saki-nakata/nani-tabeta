package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.jdbc.Sql;
import com.nanitabeta.backend.data.Item;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryTest {

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
  @Sql("/sql/insert-items.sql")
  void 商品の候補_店の商品を商品名順に取得できること() {
    List<Item> actual = sut.searchItemSuggestions(1L, null, 10);

    assertThat(actual).extracting(Item::getItemName).containsExactly("おにぎり 鮭", "からあげ棒");
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品の候補_商品が登録されていない店の場合は空のリストを返すこと() {
    List<Item> actual = sut.searchItemSuggestions(3L, null, 10);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品の候補_キーワードで部分一致の絞り込みができること() {
    List<Item> actual = sut.searchItemSuggestions(1L, "から", 10);

    assertThat(actual).extracting(Item::getId).containsExactly(1L);
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品の候補_キーワードは濁点と大文字小文字を区別しないこと() {
    List<Item> actual = sut.searchItemSuggestions(1L, "からあけ棒", 10);

    assertThat(actual).extracting(Item::getId).containsExactly(1L);
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品の候補_該当がない場合は空のリストを返すこと() {
    List<Item> actual = sut.searchItemSuggestions(1L, "ラーメン", 10);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-items.sql")
  void 商品の候補_件数の上限を超えないこと() {
    List<Item> actual = sut.searchItemSuggestions(1L, null, 1);

    assertThat(actual).hasSize(1);
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
}
