package com.nanitabeta.backend.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.nanitabeta.backend.data.Category;

/**
 * 分類マスタを扱うリポジトリです。
 */
@Mapper
public interface CategoryRepository {

  /**
   * 分類の一覧を取得します。
   *
   * @return 分類の一覧（id の昇順）
   */
  List<Category> searchCategoryList();

  /**
   * 分類を1件取得します。
   *
   * @param id 分類ID
   * @return 分類（見つからない場合は null）
   */
  Category searchCategory(Long id);
}
