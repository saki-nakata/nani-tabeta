package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.ShopRegistration;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.ShopRepository;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

  @Mock
  private ShopRepository repository;

  @InjectMocks
  private ShopService sut;

  @Test
  void 店の取得_リポジトリの結果をそのまま返すこと() {
    Shop expected = new Shop(1L, "スターバックス", 20L, 9L, false);
    when(repository.searchShop(1L)).thenReturn(expected);

    Shop actual = sut.searchShop(1L);

    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).searchShop(1L);
  }

  @Test
  void 店の取得_見つからない場合は例外を投げること() {
    when(repository.searchShop(999L)).thenReturn(null);

    assertThatThrownBy(() -> sut.searchShop(999L)).isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("店が見つかりません。id=999");
  }

  @Test
  void 店の登録_同じ店があれば登録せずにその店を返すこと() {
    Shop shop = new Shop(null, "スターバックス", 20L, 9L, null);
    Shop expected = new Shop(1L, "スターバックス", 20L, 9L, false);
    when(repository.searchShopByNameAndArea(any())).thenReturn(expected);

    ShopRegistration actual = sut.registerShop(shop);

    assertThat(actual.isCreated()).isFalse();
    assertThat(actual.getShop()).isEqualTo(expected);
    verify(repository, never()).insertShop(any());
  }

  @Test
  void 店の登録_同じ店がなければ登録すること() {
    Shop shop = new Shop(null, "スターバックス", 20L, 9L, null);
    Shop expected = new Shop(1L, "スターバックス", 20L, 9L, false);
    when(repository.searchShopByNameAndArea(any())).thenReturn(null);
    when(repository.searchShop(any())).thenReturn(expected);

    ShopRegistration actual = sut.registerShop(shop);

    assertThat(actual.isCreated()).isTrue();
    assertThat(actual.getShop()).isEqualTo(expected);
    verify(repository, times(1)).insertShop(shop);
  }

  @Test
  void 店の登録_同時に登録された場合は既存の店を返すこと() {
    Shop shop = new Shop(null, "スターバックス", 20L, 9L, null);
    Shop expected = new Shop(1L, "スターバックス", 20L, 9L, false);
    when(repository.searchShopByNameAndArea(any())).thenReturn(null);
    when(repository.searchShopByNameAndAreaForShare(any())).thenReturn(expected);
    doThrow(new DuplicateKeyException("重複")).when(repository).insertShop(any());

    ShopRegistration actual = sut.registerShop(shop);

    assertThat(actual.isCreated()).isFalse();
    assertThat(actual.getShop()).isEqualTo(expected);
  }

  @Test
  void 店の登録_店名を正規化してから探すこと() {
    Shop shop = new Shop(null, "　スターバックス　", 20L, 9L, null); // 前後に全角スペース

    sut.registerShop(shop);

    assertThat(shop.getShopName()).isEqualTo("スターバックス");
  }

  @Test
  void 店の候補_リポジトリの結果をそのまま返すこと() {
    List<Shop> expected = List.of(new Shop(1L, "スターバックス", 20L, 9L, false));
    when(repository.searchShopSuggestions(any(), any(), anyInt())).thenReturn(expected);

    List<Shop> actual = sut.searchShopSuggestions(20L, "スター");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 店の候補_キーワードを正規化してから渡すこと() {
    sut.searchShopSuggestions(20L, "　スター　バックス　"); // 前後と途中に全角スペース

    verify(repository).searchShopSuggestions(eq(20L), eq("スター バックス"), anyInt());
  }

  @Test
  void 店の候補_キーワードのワイルドカードをエスケープすること() {
    sut.searchShopSuggestions(20L, "100%_OFF");

    verify(repository).searchShopSuggestions(eq(20L), eq("100\\%\\_OFF"), anyInt());
  }

  @Test
  void 店の候補_キーワードが未指定の場合はnullのまま渡すこと() {
    sut.searchShopSuggestions(20L, null);

    verify(repository).searchShopSuggestions(eq(20L), isNull(), anyInt());
  }
}
