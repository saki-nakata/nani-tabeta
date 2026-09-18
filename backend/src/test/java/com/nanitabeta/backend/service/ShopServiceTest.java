package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.nanitabeta.backend.data.Shop;
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
}
