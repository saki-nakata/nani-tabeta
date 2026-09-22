package com.nanitabeta.backend.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nanitabeta.backend.data.Item;

/**
 * 商品を扱うリポジトリです。
 */
@Mapper
public interface ItemRepository {

  /**
   * 商品を1件取得します。
   *
   * @param id 商品ID
   * @return 商品（見つからない場合は null）
   */
  Item searchItem(Long id);

  /**
   * 指定した店の商品を、入力補助の候補として取得します。
   * <p>
   * キーワードを指定した場合は、商品名に含まれる商品だけを返します。 キーワードの比較は濁点と大文字小文字を区別しません（取りこぼしを避けるため）。
   *
   * @param shopId 店ID
   * @param keyword 商品名の一部（null または空文字の場合は絞り込まない。LIKE のエスケープ済み）
   * @param limit 取得する最大件数
   * @return 商品の一覧（該当がない場合は空のリスト）
   */
  List<Item> searchItemSuggestions(@Param("shopId") Long shopId, @Param("keyword") String keyword,
      @Param("limit") int limit);

  /**
   * 店と商品名が一致する商品を取得します。
   * <p>
   * 引数の商品のうち、店ID（shopId）と商品名（itemName）だけを使います。
   *
   * @param item 検索条件の商品
   * @return 商品（見つからない場合は null）
   */
  Item searchItemByShopAndName(Item item);

  /**
   * 店と商品名が一致する商品を、最新の確定データから取得します。
   * <p>
   * 引数の商品のうち、店ID（shopId）と商品名（itemName）だけを使います。 同時登録を検出したあとに使うため、通常の検索ではなく行ロックを取って読み込みます。
   *
   * @param item 検索条件の商品
   * @return 商品（見つからない場合は null）
   */
  Item searchItemByShopAndNameForShare(Item item);

  /**
   * 商品を登録します。
   * <p>
   * IDは自動採番で設定されます。
   *
   * @param item 商品
   */
  void insertItem(Item item);

  /**
   * 商品を更新します。
   *
   * @param item 更新する商品
   */
  void updateItem(Item item);
}
