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
}
