package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.exception.DuplicateItemNameException;
import com.nanitabeta.backend.exception.InvalidCategoryException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.ItemRepository;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

  @Mock
  private ItemRepository repository;

  @Mock
  private ShopService shopService;

  @Mock
  private CategoryService categoryService;

  @InjectMocks
  private ItemService sut;

  @Test
  void 商品の取得_リポジトリの結果をそのまま返すこと() {
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItem(1L)).thenReturn(expected);

    Item actual = sut.searchItem(1L);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 商品の取得_見つからない場合は例外を投げること() {
    when(repository.searchItem(999L)).thenReturn(null);

    assertThatThrownBy(() -> sut.searchItem(999L)).isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("商品が見つかりません。id=999");
  }

  @Test
  void 商品の候補_リポジトリの結果をそのまま返すこと() {
    List<Item> expected = List.of(new Item(1L, 1L, "からあげ棒", 4L, false));
    when(repository.searchItemSuggestions(any(), any(), anyInt())).thenReturn(expected);

    List<Item> actual = sut.searchItemSuggestions(1L, "から");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 商品の候補_店が存在しない場合は例外を投げて検索しないこと() {
    doThrow(new ResourceNotFoundException("店が見つかりません。id=999")).when(shopService)
        .searchShop(999L);

    assertThatThrownBy(() -> sut.searchItemSuggestions(999L, null))
        .isInstanceOf(ResourceNotFoundException.class).hasMessage("店が見つかりません。id=999");

    verify(repository, never()).searchItemSuggestions(any(), any(), anyInt());
  }

  @Test
  void 商品の候補_キーワードを正規化してから渡すこと() {
    sut.searchItemSuggestions(1L, "　から　あげ　"); // 前後と途中に全角スペース

    verify(repository).searchItemSuggestions(eq(1L), eq("から あげ"), anyInt());
  }

  @Test
  void 商品の候補_キーワードのワイルドカードをエスケープすること() {
    sut.searchItemSuggestions(1L, "100%_OFF");

    verify(repository).searchItemSuggestions(eq(1L), eq("100\\%\\_OFF"), anyInt());
  }

  @Test
  void 商品の候補_件数の上限を10件として渡すこと() {
    sut.searchItemSuggestions(1L, "から");

    verify(repository).searchItemSuggestions(eq(1L), eq("から"), eq(10));
  }

  @Test
  void 商品の候補_キーワードが未指定の場合はnullのまま渡すこと() {
    sut.searchItemSuggestions(1L, null);

    verify(repository).searchItemSuggestions(eq(1L), isNull(), anyInt());
  }

  @Test
  void 商品の登録_同じ商品があれば登録せずにその商品を返すこと() {
    Item item = new Item(null, 1L, "からあげ棒", 4L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItemByShopAndName(any())).thenReturn(expected);

    Item actual = sut.registerItem(item);

    assertThat(actual).isEqualTo(expected);
    verify(repository, never()).insertItem(any());
  }

  @Test
  void 商品の登録_同じ商品がなければ登録すること() {
    Item item = new Item(null, 1L, "からあげ棒", 4L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItemByShopAndName(any())).thenReturn(null);
    when(repository.searchItem(any())).thenReturn(expected);

    Item actual = sut.registerItem(item);

    assertThat(actual).isEqualTo(expected);
    verify(repository).insertItem(item);
  }

  @Test
  void 商品の登録_同時に登録された場合は既存の商品を返すこと() {
    Item item = new Item(null, 1L, "からあげ棒", 4L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItemByShopAndName(any())).thenReturn(null);
    when(repository.searchItemByShopAndNameForShare(any())).thenReturn(expected);
    doThrow(new DuplicateKeyException("重複")).when(repository).insertItem(any());

    Item actual = sut.registerItem(item);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 商品の登録_商品名を正規化してから探すこと() {
    Item item = new Item(null, 1L, "　からあげ　棒　", 4L, false); // 前後と途中に全角スペース

    sut.registerItem(item);

    assertThat(item.getItemName()).isEqualTo("からあげ 棒");
  }

  @Test
  void 商品の更新_商品名を正規化して更新し更新後の商品を返すこと() {
    Item item = new Item(1L, null, "　からあげ　棒　", 4L, false); // 前後と途中に全角スペース
    Item expected = new Item(1L, 1L, "からあげ 棒", 4L, false);
    when(repository.searchItem(1L)).thenReturn(expected);

    Item actual = sut.updateItem(item);

    assertThat(item.getItemName()).isEqualTo("からあげ 棒");
    assertThat(actual).isEqualTo(expected);
    verify(repository).updateItem(item);
  }

  @Test
  void 商品の更新_商品が見つからない場合は例外を投げて更新しないこと() {
    Item item = new Item(999L, null, "からあげ棒", 4L, false);
    when(repository.searchItem(999L)).thenReturn(null);

    assertThatThrownBy(() -> sut.updateItem(item)).isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("商品が見つかりません。id=999");

    verify(categoryService, never()).searchCategory(any()); // 商品の確認で止まり、分類は確認しない
    verify(repository, never()).updateItem(any());
  }

  @Test
  void 商品の更新_分類が存在しない場合は例外を投げて更新しないこと() {
    Item item = new Item(1L, null, "からあげ棒", 999L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItem(1L)).thenReturn(expected);
    doThrow(new InvalidCategoryException("指定された分類が存在しません。categoryId=999"))
        .when(categoryService).searchCategory(999L);

    assertThatThrownBy(() -> sut.updateItem(item)).isInstanceOf(InvalidCategoryException.class)
        .hasMessage("指定された分類が存在しません。categoryId=999");

    verify(repository, never()).updateItem(any());
  }

  @Test
  void 商品の更新_商品名が重複する場合は商品専用の例外に変換すること() {
    Item item = new Item(1L, null, "おにぎり 鮭", 1L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItem(1L)).thenReturn(expected);
    doThrow(new DuplicateKeyException("重複")).when(repository).updateItem(any());

    assertThatThrownBy(() -> sut.updateItem(item)).isInstanceOf(DuplicateItemNameException.class)
        .hasMessage("同じ商品名の商品が、その店にすでに登録されています。")
        .hasCauseInstanceOf(DuplicateKeyException.class);
  }
}
