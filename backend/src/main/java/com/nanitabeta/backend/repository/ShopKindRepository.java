package com.nanitabeta.backend.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.nanitabeta.backend.data.ShopKind;

/**
 * 業態マスタを扱うリポジトリです。
 */
@Mapper
public interface ShopKindRepository {

  /**
   * 業態の一覧を取得します。
   *
   * @return 業態の一覧（id の昇順）
   */
  List<ShopKind> searchShopKindList();

  /**
   * 業態を1件取得します。
   *
   * @param id 業態ID
   * @return 業態（見つからない場合は null）
   */
  ShopKind searchShopKind(Long id);
}

