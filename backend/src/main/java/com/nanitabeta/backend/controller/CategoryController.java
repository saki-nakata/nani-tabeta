package com.nanitabeta.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nanitabeta.backend.data.Category;
import com.nanitabeta.backend.service.CategoryService;

/**
 * 分類マスタを扱う REST API です。
 */
@RestController
public class CategoryController {

  private CategoryService service;

  @Autowired
  public CategoryController(CategoryService service) {
    this.service = service;
  }

  /**
   * 分類の一覧を取得します。
   *
   * @return 分類の一覧
   */
  @GetMapping("/api/categories")
  public List<Category> searchCategoryList() {
    return service.searchCategoryList();
  }
}