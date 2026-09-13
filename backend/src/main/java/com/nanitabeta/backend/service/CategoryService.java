package com.nanitabeta.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nanitabeta.backend.data.Category;
import com.nanitabeta.backend.repository.CategoryRepository;

/**
 * 分類マスタを扱うサービスです。
 */
@Service
public class CategoryService {

  private CategoryRepository repository;

  @Autowired
  public CategoryService(CategoryRepository repository) {
    this.repository = repository;
  }

  /**
   * 分類の一覧を取得します。
   *
   * @return 分類の一覧
   */
  public List<Category> searchCategoryList() {
    return repository.searchCategoryList();
  }
}