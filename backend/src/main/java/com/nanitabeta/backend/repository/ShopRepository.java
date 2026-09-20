package com.nanitabeta.backend.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

  /**
   * エリア内の店を、店の登録時の候補として取得します。
   * <p>
   * 閉店した店は候補に出しません。キーワードを指定した場合は、店名に含まれる店だけを返します。
   * <p>
   * キーワードの比較は濁点と大文字小文字を区別しません（取りこぼしを避けるため）。
   *
   * @param areaId エリアID
   * @param keyword 店名の一部（null または空文字の場合は絞り込まない。LIKE のエスケープ済み）
   * @param limit 取得する最大件数
   * @return 店の一覧（該当がない場合は空のリスト）
   */
  List<Shop> searchShopSuggestions(@Param("areaId") Long areaId, @Param("keyword") String keyword,
      @Param("limit") int limit);
}
