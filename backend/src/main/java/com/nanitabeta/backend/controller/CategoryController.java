package com.nanitabeta.backend.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nanitabeta.backend.data.Category;
import com.nanitabeta.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 分類マスタを扱う REST API です。
 */
@Tag(name = "分類マスタ", description = "商品の分類を扱う REST API")
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
  @Operation(summary = "分類の一覧取得", description = "分類の一覧を id の昇順で取得します。")
  @GetMapping("/api/categories")
  public List<Category> searchCategoryList() {
    return service.searchCategoryList();
  }
}
