package com.nanitabeta.backend.repository;

import com.nanitabeta.backend.data.FoodRecord;
import com.nanitabeta.backend.data.RecordPhoto;
import com.nanitabeta.backend.domain.RecordListCard;
import com.nanitabeta.backend.domain.RecordListQuery;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
   * 利用者の記録を、一覧のカードの形で取得します。
   * <p>
   * 並びは食べた日の新しい順、同じ日は記録IDの大きい順（登録の新しい順）です。 cursor を指定した場合は、その記録より後ろだけを返します。
   * <p>
   * 写真（代表写真のパス・写真の枚数）は含みません（null のまま返します）。 写真は {@link #searchRecordPhotosByRecordIdList(List)}
   * で別に取得します。
   *
   * @param query 検索の条件（絞り込み・cursor・件数）
   * @return 記録のカードの一覧（該当がない場合は空のリスト）
   */
  List<RecordListCard> searchRecordListCards(RecordListQuery query);

  /**
   * 指定した記録たちの写真を、記録ID・並び順の順でまとめて取得します。
   * <p>
   * 空のリストは渡さないでください（SQL の IN () が空になり、エラーになるため）。
   *
   * @param recordIdList 記録IDの一覧
   * @return 写真の一覧（写真がない場合は空のリスト）
   */
  List<RecordPhoto> searchRecordPhotosByRecordIdList(
      @Param("recordIdList") List<Long> recordIdList);

  /**
   * 利用者の記録がある年（食べた日の年）を、新しい順に取得します。
   *
   * @param userId 利用者のID
   * @return 年の一覧（記録がない場合は空のリスト）
   */
  List<Integer> searchRecordYears(String userId);

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
