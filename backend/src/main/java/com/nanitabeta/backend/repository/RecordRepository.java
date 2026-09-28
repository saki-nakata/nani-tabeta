package com.nanitabeta.backend.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nanitabeta.backend.data.FoodRecord;

/**
 * 記録を扱うリポジトリです。
 */
@Mapper
public interface RecordRepository {

  /**
   * 記録を1件取得します。
   *
   * @param id 記録ID
   * @return 記録（見つからない場合は null）
   */
  FoodRecord searchRecord(Long id);

  /**
   * 記録を1件取得し、行ロック（FOR UPDATE）をかけます。
   * <p>
   * 編集・削除の前に使います。トランザクションが終わるまで、同じ記録へのほかの編集・削除は待たされます。 普通の読み取り（searchRecord）は待たされません。
   *
   * @param id 記録ID
   * @return 記録（見つからない場合は null）
   */
  FoodRecord searchRecordForUpdate(Long id);

  /**
   * 記録を登録します。
   * <p>
   * IDは自動採番で設定されます。
   *
   * @param record 記録
   */
  void insertRecord(FoodRecord record);

  /**
   * 記録の写真のパスを、並び順（先頭が代表写真）で取得します。
   *
   * @param recordId 記録ID
   * @return 写真のパスの一覧（写真がない場合は空のリスト）
   */
  List<String> searchRecordPhotoPaths(Long recordId);

  /**
   * 記録の写真をまとめて登録します。
   * <p>
   * リストの順番がそのまま並び順（0 から）になります。空のリストは渡さないでください。
   *
   * @param recordId 記録ID
   * @param photoPaths 写真のパスの一覧
   */
  void insertRecordPhotos(@Param("recordId") Long recordId,
      @Param("photoPaths") List<String> photoPaths);

  /**
   * 記録を更新します。
   * <p>
   * 記録した人（user_id）は変更しません。
   *
   * @param record 更新する記録
   */
  void updateRecord(FoodRecord record);

  /**
   * 記録を削除します。
   * <p>
   * 記録の写真・いいね・コメントも、外部キー（ON DELETE CASCADE）で一緒に削除されます。
   *
   * @param id 記録ID
   */
  void deleteRecord(Long id);

  /**
   * 記録の写真を、すべて削除します。
   * <p>
   * 写真の追加・削除・並べ替えは、すべて削除してから入れ直して行います。
   *
   * @param recordId 記録ID
   */
  void deleteRecordPhotos(Long recordId);
}
