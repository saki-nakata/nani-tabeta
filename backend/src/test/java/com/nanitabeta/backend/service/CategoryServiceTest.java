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

import com.nanitabeta.backend.data.Category;
import com.nanitabeta.backend.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  @Mock
  private CategoryRepository repository;

  @InjectMocks
  private CategoryService sut;

  @Test
  void 分類の一覧取得_リポジトリの結果をそのまま返すこと() {
    List<Category> expected = List.of(new Category(1L, "ごはんもの", "🍚"));
    when(repository.searchCategoryList()).thenReturn(expected);

    List<Category> actual = sut.searchCategoryList();

    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).searchCategoryList();
  }
}