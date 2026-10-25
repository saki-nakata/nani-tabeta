package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanitabeta.backend.data.FoodRecord;
import com.nanitabeta.backend.data.RecordPhoto;
import com.nanitabeta.backend.domain.RecordListCard;
import com.nanitabeta.backend.domain.RecordListQuery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecordRepositoryTest {

  private static final String USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";

  @Autowired
  private RecordRepository sut;

  @Test
  @Sql("/sql/insert-record.sql")
  void 記録を1件取得できること() {
    FoodRecord expected =
        new FoodRecord(1L, USER_ID, 1L, LocalDate.of(2026, 9, 27), "辛さ多め", "衣がサクサク", 4, 220);

    FoodRecord actual = sut.searchRecord(1L);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 記録が見つからない場合はnullを返すこと() {
    FoodRecord actual = sut.searchRecord(999L);

    assertThat(actual).isNull();
  }

  @Test
  @Sql("/sql/insert-record.sql")
  void 記録を登録できること() {
    FoodRecord record =
        new FoodRecord(null, USER_ID, 1L, LocalDate.of(2026, 9, 26), null, null, 5, null);

    sut.insertRecord(record);

    assertThat(record.getId()).isNotNull(); // 採番された ID が書き戻されること
    FoodRecord expected =
        new FoodRecord(record.getId(), USER_ID, 1L, LocalDate.of(2026, 9, 26), null, null, 5, null);
    FoodRecord actual = sut.searchRecord(record.getId());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Sql("/sql/insert-record.sql")
  void 写真のパスを並び順で取得できること() {
    List<String> actual = sut.searchRecordPhotoPaths(1L);

    assertThat(actual).containsExactly(USER_ID + "/records/first.webp",
        USER_ID + "/records/second.webp");
  }

  @Test
  void 写真がない場合は空のリストを返すこと() {
    List<String> actual = sut.searchRecordPhotoPaths(999L);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-record.sql")
  void 写真をまとめて登録するとリストの順番が並び順になること() {
    FoodRecord record =
        new FoodRecord(null, USER_ID, 1L, LocalDate.of(2026, 9, 26), null, null, 5, null);
    sut.insertRecord(record);
    List<String> expected = List.of(USER_ID + "/records/a.webp", USER_ID + "/records/b.webp",
        USER_ID + "/records/c.webp");

    sut.insertRecordPhotos(record.getId(), expected);

    List<String> actual = sut.searchRecordPhotoPaths(record.getId());
    assertThat(actual).containsExactlyElementsOf(expected);
  }

  @Test
  @Sql("/sql/insert-record.sql")
  void 記録を更新できること_記録した人は変わらないこと() {
    // 本文に別のユーザーIDを入れても、SQL で user_id を更新しないので変わらない
    FoodRecord record = new FoodRecord(1L, "fa92b95a-22dd-4309-8f76-e53072d5d04c", 1L,
        LocalDate.of(2026, 9, 20), "氷少なめ", "また食べたい", 5, 250);
    FoodRecord expected =
        new FoodRecord(1L, USER_ID, 1L, LocalDate.of(2026, 9, 20), "氷少なめ", "また食べたい", 5, 250);

    sut.updateRecord(record);

    FoodRecord actual = sut.searchRecord(1L);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Sql("/sql/insert-record.sql")
  void 記録を削除すると記録の写真も削除されること() {
    sut.deleteRecord(1L);

    assertThat(sut.searchRecord(1L)).isNull();
    assertThat(sut.searchRecordPhotoPaths(1L)).isEmpty(); // ON DELETE CASCADE で一緒に消える
  }

  @Test
  @Sql("/sql/insert-record.sql")
  void 記録の写真をすべて削除できること() {
    sut.deleteRecordPhotos(1L);

    assertThat(sut.searchRecordPhotoPaths(1L)).isEmpty();
    assertThat(sut.searchRecord(1L)).isNotNull(); // 記録そのものは残る
  }

  @Test
  @Sql("/sql/insert-record.sql")
  void 記録を行ロックをかけて1件取得できること() {
    // FOR UPDATE が効いているか（同時操作を防げるか）までは、このテストでは確かめられない
    FoodRecord expected =
        new FoodRecord(1L, USER_ID, 1L, LocalDate.of(2026, 9, 27), "辛さ多め", "衣がサクサク", 4, 220);

    FoodRecord actual = sut.searchRecordForUpdate(1L);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 行ロックをかけて取得する記録が見つからない場合はnullを返すこと() {
    FoodRecord actual = sut.searchRecordForUpdate(999L);

    assertThat(actual).isNull();
  }

  // ここから下は記録の一覧。insert-records-for-list.sql の Saki の記録は、並びで 4・3・5・2・1

  private RecordListQuery.RecordListQueryBuilder listQuery() {
    return RecordListQuery.builder().userId(USER_ID).limit(20);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_自分の記録だけを食べた日の新しい順で同じ日はIDの大きい順に取得できること() {
    List<RecordListCard> actual = sut.searchRecordListCards(listQuery().build());

    // ほかの人の記録6（2026-10-24）は入らない
    assertThat(actual).extracting(RecordListCard::getId).containsExactly(4L, 3L, 5L, 2L, 1L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_カードに商品名と店名と分類が入り写真は入らないこと() {
    RecordListCard expected =
        new RecordListCard(4L, LocalDate.of(2026, 10, 24), 4, "Latte", 10L, "スタバ", null, null);

    List<RecordListCard> actual = sut.searchRecordListCards(listQuery().limit(1).build());

    assertThat(actual).containsExactly(expected);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_件数の上限を超えないこと() {
    List<RecordListCard> actual = sut.searchRecordListCards(listQuery().limit(2).build());

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(4L, 3L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_cursorを指定すると同じ日の続きから取得できること() {
    // 前のページの最後が記録4（2026-10-24）。同じ日の記録3から続く
    RecordListQuery query =
        listQuery().cursorEatenOn(LocalDate.of(2026, 10, 24)).cursorId(4L).build();

    List<RecordListCard> actual = sut.searchRecordListCards(query);

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(3L, 5L, 2L, 1L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_cursorを指定すると前の日の記録へ続くこと() {
    // 前のページの最後が記録3（2026-10-24 の最後）。次は 2026-10-23 の記録5
    RecordListQuery query =
        listQuery().cursorEatenOn(LocalDate.of(2026, 10, 24)).cursorId(3L).build();

    List<RecordListCard> actual = sut.searchRecordListCards(query);

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(5L, 2L, 1L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_cursor付きでもほかの人の記録が入らないこと() {
    // ほかの人の記録6は 2026-10-24 で id 6。cursor の条件（同じ日で id が 7 より小さい）には当てはまる
    RecordListQuery query =
        listQuery().cursorEatenOn(LocalDate.of(2026, 10, 24)).cursorId(7L).build();

    List<RecordListCard> actual = sut.searchRecordListCards(query);

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(4L, 3L, 5L, 2L, 1L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_分類で絞り込めること() {
    List<RecordListCard> actual = sut.searchRecordListCards(listQuery().categoryId(10L).build());

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(4L, 2L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_業態で絞り込めること() {
    List<RecordListCard> actual = sut.searchRecordListCards(listQuery().shopKindId(1L).build());

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(3L, 5L, 1L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_食べた日の範囲で絞り込めて範囲の最初と最後の日を含むこと() {
    // 2026年。2025-12-31 の記録1は入らず、2026-01-01 の記録2は入る
    RecordListQuery query =
        listQuery().eatenFrom(LocalDate.of(2026, 1, 1)).eatenTo(LocalDate.of(2026, 12, 31)).build();

    List<RecordListCard> actual = sut.searchRecordListCards(query);

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(4L, 3L, 5L, 2L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_食べた日の範囲の最後の日の記録も入ること() {
    // 2025年。2025-12-31 の記録1だけが入る
    RecordListQuery query =
        listQuery().eatenFrom(LocalDate.of(2025, 1, 1)).eatenTo(LocalDate.of(2025, 12, 31)).build();

    List<RecordListCard> actual = sut.searchRecordListCards(query);

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(1L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_業態と食べた日の範囲とcursorを組み合わせて絞り込めること() {
    // コンビニ（業態1）・2026年の記録は 3・5。cursor が記録3なので、残りは記録5だけ
    RecordListQuery query = listQuery().shopKindId(1L).eatenFrom(LocalDate.of(2026, 1, 1))
        .eatenTo(LocalDate.of(2026, 12, 31)).cursorEatenOn(LocalDate.of(2026, 10, 24)).cursorId(3L)
        .build();

    List<RecordListCard> actual = sut.searchRecordListCards(query);

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(5L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_食べた日の範囲の最初の日だけを指定してもその日以降に絞り込めること() {
    List<RecordListCard> actual =
        sut.searchRecordListCards(listQuery().eatenFrom(LocalDate.of(2026, 10, 23)).build());

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(4L, 3L, 5L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_食べた日の範囲の最後の日だけを指定してもその日以前に絞り込めること() {
    List<RecordListCard> actual =
        sut.searchRecordListCards(listQuery().eatenTo(LocalDate.of(2026, 1, 1)).build());

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(2L, 1L);
  }

  @Test
  @Sql({"/sql/insert-records-for-list.sql", "/sql/insert-records-for-list-combination.sql"})
  void 記録の一覧_分類と業態を組み合わせると両方に当てはまる記録だけになること() {
    // 記録7は分類だけ、記録8は業態だけが当てはまる
    List<RecordListCard> actual =
        sut.searchRecordListCards(listQuery().categoryId(10L).shopKindId(9L).build());

    assertThat(actual).extracting(RecordListCard::getId).containsExactly(4L, 2L);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_cursorで最後のページまでたどると重複も抜けもなく最後は空になること() {
    // 1件ずつ読むので、同じ日（2026-10-24）の記録4・3がページの境目をまたぐ
    List<Long> actual = new ArrayList<>();
    RecordListQuery query = listQuery().limit(1).build();
    for (int page = 0; page < 10; page++) {
      List<RecordListCard> cards = sut.searchRecordListCards(query);
      if (cards.isEmpty()) {
        break;
      }
      RecordListCard last = cards.get(cards.size() - 1);
      cards.forEach(card -> actual.add(card.getId()));
      query = listQuery().limit(1).cursorEatenOn(last.getEatenOn()).cursorId(last.getId()).build();
    }

    assertThat(actual).containsExactly(4L, 3L, 5L, 2L, 1L);
    assertThat(sut.searchRecordListCards(query)).isEmpty(); // 最後のページのあとは空
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_存在しない分類を指定した場合は空のリストを返すこと() {
    List<RecordListCard> actual = sut.searchRecordListCards(listQuery().categoryId(999L).build());

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の一覧_記録がない利用者の場合は空のリストを返すこと() {
    RecordListQuery query = listQuery().userId("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb").build();

    List<RecordListCard> actual = sut.searchRecordListCards(query);

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の写真_指定した記録たちの写真を記録IDと並び順の順でまとめて取得できること() {
    List<RecordPhoto> expected = List.of(new RecordPhoto(3L, USER_ID + "/records/r3-first.webp", 0),
        new RecordPhoto(4L, USER_ID + "/records/r4-first.webp", 0),
        new RecordPhoto(4L, USER_ID + "/records/r4-second.webp", 1));

    // 記録5は写真なし。ほかの人の記録6は指定していないので入らない
    List<RecordPhoto> actual = sut.searchRecordPhotosByRecordIdList(List.of(4L, 3L, 5L));

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録の写真_指定した記録に写真がない場合は空のリストを返すこと() {
    List<RecordPhoto> actual = sut.searchRecordPhotosByRecordIdList(List.of(1L, 2L));

    assertThat(actual).isEmpty();
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録がある年_自分の記録の年を重複なく新しい順に取得できること() {
    List<Integer> actual = sut.searchRecordYears(USER_ID);

    assertThat(actual).containsExactly(2026, 2025);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録がある年_ほかの人の記録の年は入らないこと() {
    // ほかの人の記録は 2026年だけ
    List<Integer> actual = sut.searchRecordYears("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    assertThat(actual).containsExactly(2026);
  }

  @Test
  @Sql("/sql/insert-records-for-list.sql")
  void 記録がある年_記録がない利用者の場合は空のリストを返すこと() {
    List<Integer> actual = sut.searchRecordYears("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");

    assertThat(actual).isEmpty();
  }
}
