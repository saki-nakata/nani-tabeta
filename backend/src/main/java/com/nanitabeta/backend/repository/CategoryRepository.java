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
}
