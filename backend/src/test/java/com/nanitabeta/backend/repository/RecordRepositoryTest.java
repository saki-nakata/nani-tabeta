package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.nanitabeta.backend.data.FoodRecord;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecordRepositoryTest {

  private static final String USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";

  @Autowired
  private RecordRepository sut;

  @Test
  @Sql("/sql/insert-record.sql")
  void 記録を1件取得できること() {
    FoodRecord expected = new FoodRecord(1L, USER_ID, 1L, LocalDate.of(2026, 9, 27), "辛さ多め",
        "衣がサクサク", 4, 220);

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
}
