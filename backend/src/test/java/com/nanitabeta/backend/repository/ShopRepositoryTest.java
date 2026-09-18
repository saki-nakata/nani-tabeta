package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
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
}
