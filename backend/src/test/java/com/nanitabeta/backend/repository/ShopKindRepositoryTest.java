package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;   // assertThat

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.nanitabeta.backend.data.ShopKind;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShopKindRepositoryTest {

  @Autowired
  private ShopKindRepository sut;

  @Test
  void 業態の一覧を取得できること() {
    List<ShopKind> actual = sut.searchShopKindList();

    assertThat(actual).hasSize(15);
    assertThat(actual.get(0).getShopKindName()).isEqualTo("コンビニ");
    assertThat(actual.get(0).getShopKindEmoji()).isEqualTo("🏪");
  }
}
