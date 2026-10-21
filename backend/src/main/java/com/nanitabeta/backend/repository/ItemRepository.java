package com.nanitabeta.backend.repository;

import com.nanitabeta.backend.data.Item;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
   * <p>
   * 商品名がキーワードと完全に一致する商品（比較は商品名の列の照合順序に従う）は、件数の上限に関係なく先頭に並べます。 残りは商品名順です。
   * 部分一致（LIKE）は1文字ずつ比べるため、完全一致になる商品が部分一致では見つからないことがあります（濁点を組み合わせた文字など）。 その場合も、完全一致する商品は候補に含めます。
   *
   * @param shopId 店ID
   * @param likeKeyword 商品名の一部（null または空文字の場合は絞り込まない。LIKE のエスケープ済み）
   * @param sameKeyword 先頭に並べる商品名（正規化済み・エスケープ前。null または空文字の場合は先頭に並べない）
   * @param limit 取得する最大件数
   * @return 商品の一覧（該当がない場合は空のリスト）
   */
  List<Item> searchItemSuggestions(@Param("shopId") Long shopId,
      @Param("likeKeyword") String likeKeyword, @Param("sameKeyword") String sameKeyword,
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

  /**
   * 商品を参照している記録が1件もない場合だけ、商品を削除します。
   * <p>
   * 記録の付け替え・削除で使われなくなった商品を消すために使います。 記録が残っている場合は何もしません。
   * <p>
   * 商品を参照するテーブル（今は records だけ）が増えた場合は、そのテーブルも条件に加えること。 加えないと、外部キー違反で削除に失敗する（ON DELETE CASCADE
   * なら参照している行が黙って消える）。
   *
   * @param id 商品ID
   */
  void deleteItemIfNoRecords(Long id);
}
