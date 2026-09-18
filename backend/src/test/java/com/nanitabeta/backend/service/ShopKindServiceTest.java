package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.nanitabeta.backend.data.ShopKind;
import com.nanitabeta.backend.repository.ShopKindRepository;

@ExtendWith(MockitoExtension.class)
class ShopKindServiceTest {

  @Mock
  private ShopKindRepository repository;

  @InjectMocks
  private ShopKindService sut;

  @Test
  void 業態の一覧取得_リポジトリの結果をそのまま返すこと() {
    List<ShopKind> expected = List.of(new ShopKind(1L, "コンビニ", "🏪"));
    when(repository.searchShopKindList()).thenReturn(expected);

    List<ShopKind> actual = sut.searchShopKindList();

    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).searchShopKindList();
  }
}
