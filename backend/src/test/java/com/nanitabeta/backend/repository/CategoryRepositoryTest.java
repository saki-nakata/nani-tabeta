package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat; // assertThat
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import com.nanitabeta.backend.data.Category; // Category

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

  @Autowired
  private CategoryRepository sut;

  @Test
  void 分類の一覧を取得できること() {
    List<Category> actual = sut.searchCategoryList();

    assertThat(actual).hasSize(11);
    assertThat(actual.get(0).getCategoryName()).isEqualTo("ごはんもの");
    assertThat(actual.get(0).getCategoryEmoji()).isEqualTo("🍚");
  }

  @Test
  void 分類を1件取得できること() {
    Category actual = sut.searchCategory(1L);

    assertThat(actual.getCategoryName()).isEqualTo("ごはんもの");
  }

  @Test
  void 分類が見つからない場合はnullを返すこと() {
    Category actual = sut.searchCategory(999L);

    assertThat(actual).isNull();
  }
}
