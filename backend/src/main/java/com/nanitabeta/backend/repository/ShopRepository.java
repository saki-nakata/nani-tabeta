package com.nanitabeta.backend.repository;

import com.nanitabeta.backend.data.Shop;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
   * <p>
   * 店名は店名の列の照合順序（utf8mb4_ja_0900_as_cs_ks）で比べます。 ひらがなとカタカナ、大文字小文字も区別するので、さくら と サクラ、CAFE と cafe
   * は別の店です。
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
   * 店を更新します。
   *
   * @param shop 更新する店
   */
  void updateShop(Shop shop);

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
   * キーワードの比較は濁点・かな・大文字小文字を区別しません（取りこぼしを避けるため）。
   * <p>
   * 並びは、完全一致（店名の列の照合順序での一致）、かな・大文字小文字の違いを吸収した一致（店名を utf8mb4_ja_0900_as_cs に切り替え、MySQL の LOWER()
   * どうしで比べた一致）、そのほかの順です。 この順に並べてから件数を絞るので、一致する店は件数の上限より前に並びます。 同じ順位の中は店名順です。
   * 店名はかなと大文字小文字を区別して登録するため（さくら と サクラ、CAFE と cafe は別の店）、書き方だけが違う店を上に出して、同じ店を別に登録する前に気づけるようにします。
   * <p>
   * 部分一致（LIKE）は1文字ずつ比べるため、かな・大文字小文字の違いを吸収して一致する店が、部分一致では見つからないことがあります（濁点を組み合わせた文字など）。
   * その場合も、その店は候補に含めます。
   *
   * @param areaId エリアID
   * @param likeKeyword 店名の一部（null または空文字の場合は絞り込まない。LIKE のエスケープ済み）
   * @param sameKeyword 先頭に並べる店名（正規化済み・エスケープ前。null または空文字の場合は先頭に並べない）
   * @param limit 取得する最大件数
   * @return 店の一覧（該当がない場合は空のリスト）
   */
  List<Shop> searchShopSuggestions(@Param("areaId") Long areaId,
      @Param("likeKeyword") String likeKeyword, @Param("sameKeyword") String sameKeyword,
      @Param("limit") int limit);
}
