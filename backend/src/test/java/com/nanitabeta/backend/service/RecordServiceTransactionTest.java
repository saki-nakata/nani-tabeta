package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.nanitabeta.backend.data.FoodRecord;
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.domain.RecordDetail;
import com.nanitabeta.backend.repository.ItemRepository;
import com.nanitabeta.backend.repository.RecordRepository;
import com.nanitabeta.backend.repository.ShopRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 本物の DB とトランザクションを使って、記録の登録を確かめるテストです。
 * <p>
 * ほかのテストと違い、トランザクションを確定させるため、専用のデータを入れて最後に削除します。
 */
@SpringBootTest
@Sql("/sql/insert-record-transaction.sql")
@Sql(scripts = "/sql/delete-record-transaction.sql",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RecordServiceTransactionTest {

  private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
  private static final Long SHOP_ID = 9001L;
  private static final String PHOTO =
      USER_ID + "/records/0b3c9f2a-1234-4abc-9def-0123456789ab.webp";

  @Autowired
  private RecordService sut;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private ShopRepository shopRepository;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @MockitoSpyBean
  private RecordRepository recordRepository; // 指定がなければ本物と同じ動きをする

  private FoodRecord input() {
    return new FoodRecord(null, USER_ID, null, LocalDate.of(2026, 9, 27), null, null, 4, null);
  }

  @Test
  void 同じ新商品が途中でほかのトランザクションに作られても登録できること() {
    // A：記録を登録する側のトランザクション
    TransactionTemplate transactionA = new TransactionTemplate(transactionManager);
    // B：途中で同じ商品を作って確定させる、別のトランザクション
    TransactionTemplate transactionB = new TransactionTemplate(transactionManager);
    transactionB.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    Item itemByB = new Item(null, SHOP_ID, "からあげ棒", 4L, false);

    RecordDetail actual = transactionA.execute(status -> {
      shopRepository.searchShop(SHOP_ID); // A の最初の SELECT。ここで最初のスナップショットが決まる
      transactionB.executeWithoutResult(statusB -> itemRepository.insertItem(itemByB)); // B が確定
      RecordDetail recordDetail =
          sut.registerRecord(input(), new Item(null, SHOP_ID, "からあげ棒", 4L, false), List.of());
      status.setRollbackOnly(); // A の登録は残さない
      return recordDetail;
    });

    assertThat(actual.getItem().getId()).isEqualTo(itemByB.getId()); // B が作った商品に記録すること
    assertThat(actual.getRecord().getItemId()).isEqualTo(itemByB.getId());
  }

  @Test
  void 大文字小文字だけが違う新商品が途中でほかのトランザクションに作られても登録できること() {
    // A は「latte」、B は「Latte」。A は UNIQUE（LOWER(商品名)）で重複になり、ロックを取って読み直す
    TransactionTemplate transactionA = new TransactionTemplate(transactionManager);
    TransactionTemplate transactionB = new TransactionTemplate(transactionManager);
    transactionB.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    Item itemByB = new Item(null, SHOP_ID, "Latte", 10L, false);

    RecordDetail actual = transactionA.execute(status -> {
      shopRepository.searchShop(SHOP_ID); // A の最初の SELECT。ここで最初のスナップショットが決まる
      transactionB.executeWithoutResult(statusB -> itemRepository.insertItem(itemByB)); // B が確定
      RecordDetail recordDetail =
          sut.registerRecord(input(), new Item(null, SHOP_ID, "latte", 10L, false), List.of());
      status.setRollbackOnly(); // A の登録は残さない
      return recordDetail;
    });

    assertThat(actual.getItem().getId()).isEqualTo(itemByB.getId()); // B が作った商品に記録すること
    assertThat(actual.getItem().getItemName()).isEqualTo("Latte");
  }

  @Test
  void 大文字小文字だけが違う既存の商品がある場合はその商品に記録し商品の内容は変えないこと() {
    // 既存の「Latte」は ドリンク（10）・季節限定。「latte」・その他（11）・季節限定でない、で記録する
    Item existing = new Item(null, SHOP_ID, "Latte", 10L, true);
    itemRepository.insertItem(existing);

    RecordDetail actual =
        sut.registerRecord(input(), new Item(null, SHOP_ID, "latte", 11L, false), List.of());

    assertThat(actual.getRecord().getItemId()).isEqualTo(existing.getId());
    Item item = itemRepository.searchItem(existing.getId());
    assertThat(item.getItemName()).isEqualTo("Latte"); // 保存されている表記のまま
    assertThat(item.getCategoryId()).isEqualTo(10L);
    assertThat(item.getIsSeasonal()).isTrue();
    Integer itemCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM items WHERE shop_id = ?",
        Integer.class, SHOP_ID);
    assertThat(itemCount).isEqualTo(1); // 新しい商品は作られていないこと
  }

  @Test
  void 途中でDBのエラーが起きた場合は商品と記録と写真をまとめて取り消すこと() {
    doThrow(new DataIntegrityViolationException("写真の登録に失敗")).when(recordRepository)
        .insertRecordPhotos(any(), any());
    Item item = new Item(null, SHOP_ID, "新しい商品", 4L, false);

    assertThatThrownBy(() -> sut.registerRecord(input(), item, List.of(PHOTO)))
        .isInstanceOf(DataIntegrityViolationException.class);

    Integer itemCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM items WHERE shop_id = ? AND item_name = ?", Integer.class, SHOP_ID,
        "新しい商品");
    Integer recordCount = jdbcTemplate
        .queryForObject("SELECT COUNT(*) FROM records WHERE user_id = ?", Integer.class, USER_ID);
    assertThat(itemCount).isZero(); // 先に作った商品も取り消されていること
    assertThat(recordCount).isZero(); // 記録も残っていないこと
  }
}
