package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.nanitabeta.backend.data.FoodRecord;
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.domain.RecordUpdateResult;
import com.nanitabeta.backend.exception.RecordVersionConflictException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

/**
 * 本物の DB とトランザクションを使って、記録の編集の版（version）の確認を確かめるテストです。
 * <p>
 * 2 つの画面が同じ版から保存したとき、片方だけが成功し、もう片方は 409 用の例外になって、商品・写真の変更が DB に残らないことを確かめます。 古い版の保存は、Service
 * が商品を登録する前に版を比べて断るので、この経路では商品を作ってから取り消すのではなく、作らずに止まります （Repository が 0
 * 件を返したときに取り消す処理は、RecordServiceTest で確かめています）。 トランザクションを確定させるため、専用のデータを入れて最後に削除します。
 */
@SpringBootTest
@Sql({"/sql/insert-record-transaction.sql", "/sql/insert-record-for-version.sql"})
@Sql(scripts = "/sql/delete-record-transaction.sql",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RecordVersionConflictTest {

  private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
  private static final Long SHOP_ID = 9001L;
  private static final Long RECORD_ID = 9201L;
  private static final String PHOTO_P =
      USER_ID + "/records/aaaaaaaa-0000-4000-8000-000000000001.webp";
  private static final String PHOTO_Q =
      USER_ID + "/records/aaaaaaaa-0000-4000-8000-000000000002.webp";
  private static final String PHOTO_R =
      USER_ID + "/records/aaaaaaaa-0000-4000-8000-000000000003.webp";

  @Autowired
  private RecordService sut;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @MockitoSpyBean
  private ItemService itemService; // 指定がなければ本物と同じ動きをする

  /** 編集画面から届く記録（version は、編集画面が読んだときの版） */
  private FoodRecord input(String review, int version) {
    return new FoodRecord(null, USER_ID, null, LocalDate.of(2026, 9, 27), null, review, 4, null,
        version);
  }

  /** 店 9001 の商品（名前が新しければ、保存のときに作られる） */
  private Item item(String itemName) {
    return new Item(null, SHOP_ID, itemName, 4L, false);
  }

  private String reviewInDb() {
    return jdbcTemplate.queryForObject("SELECT review FROM records WHERE id = ?", String.class,
        RECORD_ID);
  }

  private Integer versionInDb() {
    return jdbcTemplate.queryForObject("SELECT version FROM records WHERE id = ?", Integer.class,
        RECORD_ID);
  }

  private List<String> photosInDb() {
    return jdbcTemplate.queryForList(
        "SELECT photo_path FROM record_photos WHERE record_id = ? ORDER BY sort_order",
        String.class, RECORD_ID);
  }

  private Integer itemCount(String itemName) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM items WHERE shop_id = ? AND item_name = ?", Integer.class, SHOP_ID,
        itemName);
  }

  @Test
  void 同じ版から順番に2回保存すると1回目だけ成功し2回目の商品と写真の変更は残らないこと() {
    // 1 回目（画面 A）：q を外す
    RecordUpdateResult first =
        sut.updateRecord(RECORD_ID, input("画面A", 0), item("からあげ棒"), List.of(PHOTO_P));

    // 2 回目（画面 B）：同じ版 0 のまま、新しい商品にして、写真を p・q・r にしようとする
    assertThatThrownBy(() -> sut.updateRecord(RECORD_ID, input("画面B", 0), item("画面Bの新しい商品"),
        List.of(PHOTO_P, PHOTO_Q, PHOTO_R))).isInstanceOf(RecordVersionConflictException.class);

    assertThat(first.getRemovedPhotoPaths()).containsExactly(PHOTO_Q); // A が外した写真
    assertThat(reviewInDb()).isEqualTo("画面A"); // A の内容のまま
    assertThat(versionInDb()).isEqualTo(1); // A の保存で 1 増えただけ
    assertThat(photosInDb()).containsExactly(PHOTO_P); // A の写真が残り、B の写真は入っていない
    assertThat(itemCount("画面Bの新しい商品")).isZero(); // B は商品を作る前に断られ、商品は作られていない
  }

  @Test
  void 同じ版から同時に2回保存すると片方だけ成功しもう片方の変更は残らないこと() throws Exception {
    // 2 つの保存を同じ瞬間に始め（bothReady）、先に行ロックを取った側は、商品を決める所で少し止める
    // （その間に、もう片方が行ロックを待つように、重なりやすくしている。ロック待ちに入ったことまでは確かめていない）
    CyclicBarrier bothReady = new CyclicBarrier(2);
    doAnswer(invocation -> {
      Thread.sleep(500);
      return invocation.callRealMethod();
    }).when(itemService).registerItem(any());

    ExecutorService executor = Executors.newFixedThreadPool(2);
    List<String> succeeded = Collections.synchronizedList(new ArrayList<>());
    List<Throwable> failed = Collections.synchronizedList(new ArrayList<>());
    List<Future<?>> futures = new ArrayList<>();
    for (String screen : List.of("A", "B")) {
      futures.add(executor.submit(() -> {
        try {
          bothReady.await(10, TimeUnit.SECONDS);
          // A は p だけ、B は q だけを残す。新しい商品もそれぞれ作ろうとする
          List<String> photos = screen.equals("A") ? List.of(PHOTO_P) : List.of(PHOTO_Q);
          sut.updateRecord(RECORD_ID, input("画面" + screen, 0), item("画面" + screen + "の商品"), photos);
          succeeded.add(screen);
        } catch (Throwable ex) {
          failed.add(ex);
        }
      }));
    }
    for (Future<?> future : futures) {
      future.get(30, TimeUnit.SECONDS);
    }
    executor.shutdown();

    assertThat(succeeded).as("失敗した理由: %s", failed).hasSize(1);
    assertThat(failed).hasSize(1);
    assertThat(failed.get(0)).isInstanceOf(RecordVersionConflictException.class);
    String winner = succeeded.get(0);
    String loser = winner.equals("A") ? "B" : "A";
    assertThat(reviewInDb()).isEqualTo("画面" + winner); // 成功した側の内容
    assertThat(versionInDb()).isEqualTo(1); // 1 回分だけ増えた
    List<String> expectedPhotos = winner.equals("A") ? List.of(PHOTO_P) : List.of(PHOTO_Q);
    assertThat(photosInDb()).isEqualTo(expectedPhotos); // 成功した側の写真が残っている
    assertThat(itemCount("画面" + winner + "の商品")).isEqualTo(1);
    // 失敗した側は、ロックが外れるのを待ったあと、版を比べた所で断られ、商品は作られていない
    assertThat(itemCount("画面" + loser + "の商品")).isZero();
  }

  @Test
  void 写真を外して保存したあとに古い画面から保存しても409になり外した写真は戻らないこと() {
    // 画面 A：q を外して保存（このあと q は Storage から消される）
    RecordUpdateResult first =
        sut.updateRecord(RECORD_ID, input("画面A", 0), item("からあげ棒"), List.of(PHOTO_P));
    assertThat(first.getRemovedPhotoPaths()).containsExactly(PHOTO_Q);

    // 画面 B：A の保存の前に開いた古い画面（版 0）。まだ q が並んでいるまま、感想だけ変えて保存
    assertThatThrownBy(() -> sut.updateRecord(RECORD_ID, input("画面B", 0), item("からあげ棒"),
        List.of(PHOTO_P, PHOTO_Q))).isInstanceOf(RecordVersionConflictException.class);

    assertThat(photosInDb()).containsExactly(PHOTO_P); // 消した q が、記録に戻っていない
    assertThat(reviewInDb()).isEqualTo("画面A");
  }

  @Test
  void 最新の版から保存し直せば成功すること() {
    sut.updateRecord(RECORD_ID, input("画面A", 0), item("からあげ棒"), List.of(PHOTO_P));

    // 画面を開き直した（版 1 を読んだ）あとなら保存できる
    RecordUpdateResult actual =
        sut.updateRecord(RECORD_ID, input("開き直した画面", 1), item("からあげ棒"), List.of(PHOTO_P, PHOTO_R));

    assertThat(actual.getRecordDetail().getRecord().getVersion()).isEqualTo(2);
    assertThat(actual.getRemovedPhotoPaths()).isEmpty();
    assertThat(photosInDb()).containsExactly(PHOTO_P, PHOTO_R);
  }
}
