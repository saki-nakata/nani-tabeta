package com.nanitabeta.backend.repository;

import org.apache.ibatis.annotations.Mapper;
import com.nanitabeta.backend.data.Shop;

/**
 * 店を扱うリポジトリです。
 */
@Mapper
public interface ShopRepository {
  /**
   * 店を1件取得します。
   *
   * @param id 店ID
   * @return 店（見つからない場合は null）
   */
  Shop searchShop(Long id);
}
