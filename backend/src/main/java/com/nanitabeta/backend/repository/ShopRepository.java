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

  /**
   * 店名とエリアが一致する店を取得します。
   * <p>
   * 引数の店のうち、店名（shopName）とエリアID（areaId）だけを使います。
   *
   * @param shop 検索条件の店
   * @return 店（見つからない場合は null）
   */
  Shop searchShopByNameAndArea(Shop shop);

  /**
   * 店を登録します。
   * <p>
   * IDは自動採番で設定されます。
   *
   * @param shop 店
   */
  void insertShop(Shop shop);

  /**
   * 店名とエリアが一致する店を、最新の確定データから取得します。
   * <p>
   * 引数の店のうち、店名（shopName）とエリアID（areaId）だけを使います。 同時登録を検出したあとに使うため、通常の検索ではなく行ロックを取って読み込みます。
   *
   * @param shop 検索条件の店
   * @return 店（見つからない場合は null）
   */
  Shop searchShopByNameAndAreaForShare(Shop shop);
}
