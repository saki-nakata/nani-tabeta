package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanitabeta.backend.data.FoodRecord;
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.data.RecordPhoto;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.RecordDetail;
import com.nanitabeta.backend.domain.RecordListCard;
import com.nanitabeta.backend.domain.RecordListCursor;
import com.nanitabeta.backend.domain.RecordListPage;
import com.nanitabeta.backend.domain.RecordListQuery;
import com.nanitabeta.backend.exception.ForbiddenException;
import com.nanitabeta.backend.exception.InvalidCategoryException;
import com.nanitabeta.backend.exception.InvalidPhotoPathException;
import com.nanitabeta.backend.exception.InvalidSearchConditionException;
import com.nanitabeta.backend.exception.InvalidShopException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.RecordRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

  private static final String USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";
  private static final String PHOTO_1 =
      USER_ID + "/records/0b3c9f2a-1234-4abc-9def-0123456789ab.webp";
  private static final String PHOTO_2 =
      USER_ID + "/records/7e1d2c3b-5678-4def-8abc-ba9876543210.webp";
  private static final LocalDate EATEN_ON = LocalDate.of(2026, 9, 27);
  private static final String OTHER_USER_ID = "fa92b95a-22dd-4309-8f76-e53072d5d04c";
  private static final LocalDate OCTOBER_24 = LocalDate.of(2026, 10, 24);
  private static final LocalDate OCTOBER_20 = LocalDate.of(2026, 10, 20);
  private static final String CONDITION_MESSAGE_CURSOR =
      "cursorEatenOn と cursorId は、両方指定するか、両方省略してください。";

  @Mock
  private RecordRepository repository;

  @Mock
  private ItemService itemService;

  @Mock
  private ShopService shopService;

  @Mock
  private CategoryService categoryService;

  @InjectMocks
  private RecordService sut;

  @Test
  void 記録の詳細_記録と商品と店と写真をまとめて返すこと() {
    FoodRecord record = new FoodRecord(1L, USER_ID, 6L, EATEN_ON, null, "衣がサクサク", 4, 220);
    Item item = new Item(6L, 2L, "からあげ棒", 4L, false);
    Shop shop = new Shop(2L, "セブンイレブン", 20L, 1L, false);
    when(repository.searchRecord(1L)).thenReturn(record);
    when(itemService.searchItem(6L)).thenReturn(item);
    when(shopService.searchShop(2L)).thenReturn(shop);
    when(repository.searchRecordPhotoPaths(1L)).thenReturn(List.of(PHOTO_1));
    RecordDetail expected = new RecordDetail(record, item, shop, List.of(PHOTO_1));

    RecordDetail actual = sut.searchRecordDetail(1L);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 記録の詳細_見つからない場合は例外を投げること() {
    when(repository.searchRecord(999L)).thenReturn(null);

    assertThatThrownBy(() -> sut.searchRecordDetail(999L))
        .isInstanceOf(ResourceNotFoundException.class).hasMessage("記録が見つかりません。id=999");

    verify(itemService, never()).searchItem(any());
  }

  @Test
  void 記録の登録_商品を用意してから記録と写真を登録し記録の詳細を返すこと() {
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, "衣がサクサク", 4, 220);
    Item item = new Item(null, 2L, "からあげ棒", 4L, false);
    Item linkedItem = new Item(6L, 2L, "からあげ棒", 4L, false);
    Shop shop = new Shop(2L, "セブンイレブン", 20L, 1L, false);
    when(shopService.searchShopToLink(2L)).thenReturn(shop);
    when(itemService.registerItem(item)).thenReturn(linkedItem);
    doAnswer(invocation -> {
      FoodRecord inserted = invocation.getArgument(0);
      inserted.setId(1L); // DB が振った ID を書き戻す（useGeneratedKeys の代わり）
      return null;
    }).when(repository).insertRecord(any());
    FoodRecord registered = new FoodRecord(1L, USER_ID, 6L, EATEN_ON, null, "衣がサクサク", 4, 220);
    when(repository.searchRecord(1L)).thenReturn(registered);
    when(repository.searchRecordPhotoPaths(1L)).thenReturn(List.of(PHOTO_1, PHOTO_2));
    // insertRecord に渡した記録（ID は INSERT で書き戻された値）
    FoodRecord expectedArgument = new FoodRecord(1L, USER_ID, 6L, EATEN_ON, null, "衣がサクサク", 4, 220);
    RecordDetail expected =
        new RecordDetail(registered, linkedItem, shop, List.of(PHOTO_1, PHOTO_2));

    RecordDetail actual = sut.registerRecord(input, item, List.of(PHOTO_1, PHOTO_2));

    assertThat(actual).isEqualTo(expected);
    verify(repository).insertRecord(expectedArgument); // 用意した商品の ID で記録すること
    verify(repository).insertRecordPhotos(1L, List.of(PHOTO_1, PHOTO_2)); // 採番された ID で写真を登録すること
    // 商品と店は取り直さない（最初のスナップショットでは、途中で作られた商品が見えないことがあるため）
    verify(itemService, never()).searchItem(any());
    verify(shopService, never()).searchShop(any());
    assertThat(input.getItemId()).isNull(); // 受け取った引数は変更しないこと
    assertThat(input.getId()).isNull();
  }

  @Test
  void 記録の登録_写真が指定されていない場合は写真を登録しないこと() {
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, null, 4, null);
    Item item = new Item(null, 2L, "からあげ棒", 4L, false);
    when(itemService.registerItem(item)).thenReturn(new Item(6L, 2L, "からあげ棒", 4L, false));
    when(repository.searchRecord(any()))
        .thenReturn(new FoodRecord(1L, USER_ID, 6L, EATEN_ON, null, null, 4, null));

    sut.registerRecord(input, item, null);

    verify(repository).insertRecord(any());
    verify(repository, never()).insertRecordPhotos(any(), any());
  }

  @Test
  void 記録の登録_写真が空のリストの場合は写真を登録しないこと() {
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, null, 4, null);
    Item item = new Item(null, 2L, "からあげ棒", 4L, false);
    when(itemService.registerItem(item)).thenReturn(new Item(6L, 2L, "からあげ棒", 4L, false));
    when(repository.searchRecord(any()))
        .thenReturn(new FoodRecord(1L, USER_ID, 6L, EATEN_ON, null, null, 4, null));

    sut.registerRecord(input, item, List.of());

    verify(repository, never()).insertRecordPhotos(any(), any());
  }

  @Test
  void 記録の登録_店が存在しない場合は例外を投げて何も登録しないこと() {
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, null, 4, null);
    Item item = new Item(null, 999L, "からあげ棒", 4L, false);
    doThrow(new InvalidShopException("指定された店が存在しません。shopId=999")).when(shopService)
        .searchShopToLink(999L);

    assertThatThrownBy(() -> sut.registerRecord(input, item, List.of(PHOTO_1)))
        .isInstanceOf(InvalidShopException.class).hasMessage("指定された店が存在しません。shopId=999");

    verify(categoryService, never()).searchCategory(any()); // 店の確認で止まり、分類は確認しない
    verify(itemService, never()).registerItem(any());
    verify(repository, never()).insertRecord(any());
  }

  @Test
  void 記録の登録_分類が存在しない場合は例外を投げて何も登録しないこと() {
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, null, 4, null);
    Item item = new Item(null, 2L, "からあげ棒", 999L, false);
    doThrow(new InvalidCategoryException("指定された分類が存在しません。categoryId=999")).when(categoryService)
        .searchCategory(999L);

    assertThatThrownBy(() -> sut.registerRecord(input, item, List.of(PHOTO_1)))
        .isInstanceOf(InvalidCategoryException.class);

    verify(itemService, never()).registerItem(any());
    verify(repository, never()).insertRecord(any());
  }

  @Test
  void 記録の登録_写真のパスが正しくない場合は例外を投げて何も登録しないこと() {
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, null, 4, null);
    Item item = new Item(null, 2L, "からあげ棒", 4L, false);
    String otherUsersPhoto =
        "fa92b95a-22dd-4309-8f76-e53072d5d04c/records/0b3c9f2a-1234-4abc-9def-0123456789ab.webp";

    assertThatThrownBy(() -> sut.registerRecord(input, item, List.of(otherUsersPhoto)))
        .isInstanceOf(InvalidPhotoPathException.class);

    verify(itemService, never()).registerItem(any()); // 商品を作る前に止まること
    verify(repository, never()).insertRecord(any());
  }

  @Test
  void 記録の編集_記録を書き換えて写真を入れ直し記録の詳細を返すこと() {
    FoodRecord myRecord = new FoodRecord(2L, USER_ID, 6L, EATEN_ON, null, "衣がサクサク", 4, 220);
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, "少し冷めていた", 3, 250);
    Item item = new Item(null, 2L, "からあげ棒", 4L, false);
    Item linkedItem = new Item(6L, 2L, "からあげ棒", 4L, false); // 商品は変わらない
    Shop shop = new Shop(2L, "セブンイレブン", 20L, 1L, false);
    FoodRecord updated = new FoodRecord(2L, USER_ID, 6L, EATEN_ON, null, "少し冷めていた", 3, 250);
    when(repository.searchRecordForUpdate(2L)).thenReturn(myRecord); // 最初の確認（行ロック）
    when(repository.searchRecord(2L)).thenReturn(updated); // 最後の取り直し
    when(shopService.searchShopToLink(2L)).thenReturn(shop);
    when(itemService.registerItem(item)).thenReturn(linkedItem);
    when(repository.searchRecordPhotoPaths(2L)).thenReturn(List.of(PHOTO_2, PHOTO_1));
    FoodRecord expectedArgument =
        new FoodRecord(2L, USER_ID, 6L, EATEN_ON, null, "少し冷めていた", 3, 250);
    RecordDetail expected = new RecordDetail(updated, linkedItem, shop, List.of(PHOTO_2, PHOTO_1));

    RecordDetail actual = sut.updateRecord(2L, input, item, List.of(PHOTO_2, PHOTO_1));

    assertThat(actual).isEqualTo(expected);
    verify(repository).updateRecord(expectedArgument);
    InOrder photos = inOrder(repository);
    photos.verify(repository).deleteRecordPhotos(2L); // 全部消してから
    photos.verify(repository).insertRecordPhotos(2L, List.of(PHOTO_2, PHOTO_1)); // 送られた順に入れ直す
    verify(itemService, never()).deleteItemIfNoRecords(any()); // 付け替えていないので消さない
    assertThat(input.getId()).isNull(); // 受け取った引数は変更しないこと
  }

  @Test
  void 記録の編集_別の商品に付け替えた場合は前の商品の後始末をすること() {
    FoodRecord myRecord = new FoodRecord(2L, USER_ID, 7L, EATEN_ON, null, null, 4, null);
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, null, 4, null);
    Item item = new Item(null, 2L, "からあげ棒", 4L, false);
    when(repository.searchRecordForUpdate(2L)).thenReturn(myRecord);
    when(itemService.registerItem(item)).thenReturn(new Item(6L, 2L, "からあげ棒", 4L, false));

    sut.updateRecord(2L, input, item, List.of());

    verify(itemService).deleteItemIfNoRecords(7L); // 前の商品（7）を消しに行く
  }

  @Test
  void 記録の編集_写真をすべて外した場合は写真を消すだけで入れ直さないこと() {
    FoodRecord myRecord = new FoodRecord(2L, USER_ID, 6L, EATEN_ON, null, null, 4, null);
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, null, 4, null);
    Item item = new Item(null, 2L, "からあげ棒", 4L, false);
    when(repository.searchRecordForUpdate(2L)).thenReturn(myRecord);
    when(itemService.registerItem(item)).thenReturn(new Item(6L, 2L, "からあげ棒", 4L, false));

    sut.updateRecord(2L, input, item, List.of());

    verify(repository).deleteRecordPhotos(2L);
    verify(repository, never()).insertRecordPhotos(any(), any());
  }

  @Test
  void 記録の編集_記録が見つからない場合は404用の例外を投げて何も変更しないこと() {
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, null, 4, null);
    when(repository.searchRecordForUpdate(999L)).thenReturn(null);

    assertThatThrownBy(
        () -> sut.updateRecord(999L, input, new Item(null, 2L, "からあげ棒", 4L, false), null))
        .isInstanceOf(ResourceNotFoundException.class).hasMessage("記録が見つかりません。id=999");

    verify(shopService, never()).searchShopToLink(any());
    verify(repository, never()).updateRecord(any());
  }

  @Test
  void 記録の編集_他人の記録の場合は403用の例外を投げて何も変更しないこと() {
    FoodRecord othersRecord = new FoodRecord(3L, OTHER_USER_ID, 5L, EATEN_ON, null, null, 3, null);
    FoodRecord input = new FoodRecord(null, USER_ID, null, EATEN_ON, null, null, 1, null);
    when(repository.searchRecordForUpdate(3L)).thenReturn(othersRecord);

    assertThatThrownBy(
        () -> sut.updateRecord(3L, input, new Item(null, 999L, "おにぎり", 1L, false), null))
        .isInstanceOf(ForbiddenException.class).hasMessage("この記録を変更する権限がありません。id=3");

    // 持ち主の確認で止まり、本文の確認（店 999 は存在しない）まで進まないこと
    verify(shopService, never()).searchShopToLink(any());
    verify(repository, never()).updateRecord(any());
    verify(repository, never()).deleteRecordPhotos(any());
  }

  @Test
  void 記録の削除_写真のパスを読んでから記録を削除し商品の後始末をすること() {
    FoodRecord myRecord = new FoodRecord(1L, USER_ID, 6L, EATEN_ON, null, null, 4, null);
    when(repository.searchRecordForUpdate(1L)).thenReturn(myRecord);
    when(repository.searchRecordPhotoPaths(1L)).thenReturn(List.of(PHOTO_1, PHOTO_2));

    List<String> actual = sut.deleteRecord(1L, USER_ID);

    assertThat(actual).containsExactly(PHOTO_1, PHOTO_2);
    InOrder order = inOrder(repository, itemService);
    order.verify(repository).searchRecordPhotoPaths(1L); // 消す前に写真のパスを読む
    order.verify(repository).deleteRecord(1L);
    order.verify(itemService).deleteItemIfNoRecords(6L); // 記録を消したあとで商品の後始末
  }

  @Test
  void 記録の削除_記録が見つからない場合は404用の例外を投げて何も削除しないこと() {
    when(repository.searchRecordForUpdate(999L)).thenReturn(null);

    assertThatThrownBy(() -> sut.deleteRecord(999L, USER_ID))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(repository, never()).deleteRecord(any());
  }

  @Test
  void 記録の削除_他人の記録の場合は403用の例外を投げて何も削除しないこと() {
    when(repository.searchRecordForUpdate(3L))
        .thenReturn(new FoodRecord(3L, OTHER_USER_ID, 5L, EATEN_ON, null, null, 3, null));

    assertThatThrownBy(() -> sut.deleteRecord(3L, USER_ID)).isInstanceOf(ForbiddenException.class)
        .hasMessage("この記録を変更する権限がありません。id=3");

    verify(repository, never()).deleteRecord(any());
    verify(itemService, never()).deleteItemIfNoRecords(any());
  }

  /** 一覧の SQL が返すカード（写真の項目はまだ入っていない） */
  private static RecordListCard card(Long id, LocalDate eatenOn) {
    return new RecordListCard(id, eatenOn, 4, "からあげ棒", 4L, "セブンイレブン", null, null);
  }

  /** 写真をつなげたあとのカード */
  private static RecordListCard cardWithPhotos(Long id, LocalDate eatenOn, String mainPhotoPath,
      int photoCount) {
    return new RecordListCard(id, eatenOn, 4, "からあげ棒", 4L, "セブンイレブン", mainPhotoPath, photoCount);
  }

  /** Repository に渡した検索の条件を取り出す */
  private RecordListQuery capturedQuery() {
    ArgumentCaptor<RecordListQuery> captor = ArgumentCaptor.forClass(RecordListQuery.class);
    verify(repository).searchRecordListCards(captor.capture());
    return captor.getValue();
  }

  @Test
  void 記録の一覧_続きがない場合は取得したカードをすべて返し次の目印はnullにすること() {
    when(repository.searchRecordListCards(any()))
        .thenReturn(List.of(card(12L, OCTOBER_24), card(9L, OCTOBER_20)));
    when(repository.searchRecordPhotosByRecordIdList(List.of(12L, 9L))).thenReturn(List.of());
    RecordListPage expected = new RecordListPage(
        List.of(cardWithPhotos(12L, OCTOBER_24, null, 0), cardWithPhotos(9L, OCTOBER_20, null, 0)),
        null);
    // 絞り込みはそのまま渡し、件数は続きがあるかを確かめるため 1 件多くする
    RecordListQuery expectedQuery =
        RecordListQuery.builder().userId(USER_ID).categoryId(4L).shopKindId(1L).limit(21).build();

    RecordListPage actual = sut.searchRecordList(USER_ID, 4L, 1L, null, null, null, 20);

    assertThat(actual).isEqualTo(expected);
    assertThat(capturedQuery()).usingRecursiveComparison().isEqualTo(expectedQuery);
  }

  @Test
  void 記録の一覧_続きがある場合はlimit件だけ返し最後のカードから次の目印を作ること() {
    when(repository.searchRecordListCards(any()))
        .thenReturn(List.of(card(12L, OCTOBER_24), card(9L, OCTOBER_20), card(7L, OCTOBER_20)));
    when(repository.searchRecordPhotosByRecordIdList(List.of(12L, 9L))).thenReturn(List.of());
    RecordListPage expected = new RecordListPage(
        List.of(cardWithPhotos(12L, OCTOBER_24, null, 0), cardWithPhotos(9L, OCTOBER_20, null, 0)),
        new RecordListCursor(OCTOBER_20, 9L));

    RecordListPage actual = sut.searchRecordList(USER_ID, null, null, null, null, null, 2);

    assertThat(actual).isEqualTo(expected);
    // 確かめるために多く読んだ 1 件（記録 7）の写真は検索しないこと
    verify(repository).searchRecordPhotosByRecordIdList(List.of(12L, 9L));
  }

  @Test
  void 記録の一覧_年はその年の1月1日から12月31日までの範囲にしcursorはそのまま渡すこと() {
    RecordListQuery expectedQuery = RecordListQuery.builder().userId(USER_ID)
        .eatenFrom(LocalDate.of(2026, 1, 1)).eatenTo(LocalDate.of(2026, 12, 31))
        .cursorEatenOn(OCTOBER_20).cursorId(9L).limit(21).build();

    sut.searchRecordList(USER_ID, null, null, 2026, OCTOBER_20, 9L, 20);

    assertThat(capturedQuery()).usingRecursiveComparison().isEqualTo(expectedQuery);
  }

  @Test
  void 記録の一覧_写真はカードの順のままつなげ代表写真は並び順が最小のものにすること() {
    String photo3 = USER_ID + "/records/3c4d5e6f-9abc-4def-8123-456789abcdef.webp";
    when(repository.searchRecordListCards(any()))
        .thenReturn(List.of(card(12L, OCTOBER_24), card(9L, OCTOBER_20), card(7L, OCTOBER_20)));
    // 写真の SQL は記録ID の順。記録 12 は、並び順 1 の写真が先に来ても 0 の写真を代表にすること
    when(repository.searchRecordPhotosByRecordIdList(List.of(12L, 9L, 7L)))
        .thenReturn(List.of(new RecordPhoto(9L, photo3, 0), new RecordPhoto(12L, PHOTO_2, 1),
            new RecordPhoto(12L, PHOTO_1, 0)));
    RecordListPage expected = new RecordListPage(
        List.of(cardWithPhotos(12L, OCTOBER_24, PHOTO_1, 2),
            cardWithPhotos(9L, OCTOBER_20, photo3, 1), cardWithPhotos(7L, OCTOBER_20, null, 0)),
        null);

    RecordListPage actual = sut.searchRecordList(USER_ID, null, null, null, null, null, 20);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 記録の一覧_カードが0件の場合は写真を検索せず空の一覧を返すこと() {
    when(repository.searchRecordListCards(any())).thenReturn(List.of());
    RecordListPage expected = new RecordListPage(List.of(), null);

    RecordListPage actual = sut.searchRecordList(USER_ID, 999L, null, null, null, null, 20);

    assertThat(actual).isEqualTo(expected);
    verify(repository, never()).searchRecordPhotosByRecordIdList(any());
  }

  @Test
  void 記録の一覧_分類IDが0の場合は例外を投げて検索しないこと() {
    assertThatThrownBy(() -> sut.searchRecordList(USER_ID, 0L, null, null, null, null, 20))
        .isInstanceOf(InvalidSearchConditionException.class).hasMessage("分類IDは1以上で指定してください。");

    verify(repository, never()).searchRecordListCards(any());
  }

  @Test
  void 記録の一覧_業態IDが負の場合は例外を投げて検索しないこと() {
    assertThatThrownBy(() -> sut.searchRecordList(USER_ID, null, -1L, null, null, null, 20))
        .isInstanceOf(InvalidSearchConditionException.class).hasMessage("業態IDは1以上で指定してください。");

    verify(repository, never()).searchRecordListCards(any());
  }

  @Test
  void 記録の一覧_年が1000より前の場合は例外を投げて検索しないこと() {
    assertThatThrownBy(() -> sut.searchRecordList(USER_ID, null, null, 999, null, null, 20))
        .isInstanceOf(InvalidSearchConditionException.class).hasMessage("年は1000〜9999で指定してください。");

    verify(repository, never()).searchRecordListCards(any());
  }

  @Test
  void 記録の一覧_年が9999より後の場合は例外を投げて検索しないこと() {
    assertThatThrownBy(() -> sut.searchRecordList(USER_ID, null, null, 10000, null, null, 20))
        .isInstanceOf(InvalidSearchConditionException.class).hasMessage("年は1000〜9999で指定してください。");

    verify(repository, never()).searchRecordListCards(any());
  }

  @Test
  void 記録の一覧_年が1000と9999の場合は検索できること() {
    sut.searchRecordList(USER_ID, null, null, 1000, null, null, 20);
    sut.searchRecordList(USER_ID, null, null, 9999, null, null, 20);

    verify(repository).searchRecordListCards(
        argThat(query -> LocalDate.of(1000, 1, 1).equals(query.getEatenFrom())));
    verify(repository).searchRecordListCards(
        argThat(query -> LocalDate.of(9999, 12, 31).equals(query.getEatenTo())));
  }

  @Test
  void 記録の一覧_cursorの食べた日だけ指定された場合は例外を投げて検索しないこと() {
    assertThatThrownBy(() -> sut.searchRecordList(USER_ID, null, null, null, OCTOBER_20, null, 20))
        .isInstanceOf(InvalidSearchConditionException.class).hasMessage(CONDITION_MESSAGE_CURSOR);

    verify(repository, never()).searchRecordListCards(any());
  }

  @Test
  void 記録の一覧_cursorの記録IDだけ指定された場合は例外を投げて検索しないこと() {
    assertThatThrownBy(() -> sut.searchRecordList(USER_ID, null, null, null, null, 9L, 20))
        .isInstanceOf(InvalidSearchConditionException.class).hasMessage(CONDITION_MESSAGE_CURSOR);

    verify(repository, never()).searchRecordListCards(any());
  }

  @Test
  void 記録の一覧_cursorの記録IDが0の場合は例外を投げて検索しないこと() {
    assertThatThrownBy(() -> sut.searchRecordList(USER_ID, null, null, null, OCTOBER_20, 0L, 20))
        .isInstanceOf(InvalidSearchConditionException.class).hasMessage("cursorIdは1以上で指定してください。");

    verify(repository, never()).searchRecordListCards(any());
  }

  @Test
  void 記録の一覧_件数が0の場合は例外を投げて検索しないこと() {
    assertThatThrownBy(() -> sut.searchRecordList(USER_ID, null, null, null, null, null, 0))
        .isInstanceOf(InvalidSearchConditionException.class).hasMessage("件数は1〜100で指定してください。");

    verify(repository, never()).searchRecordListCards(any());
  }

  @Test
  void 記録の一覧_件数が101の場合は例外を投げて検索しないこと() {
    assertThatThrownBy(() -> sut.searchRecordList(USER_ID, null, null, null, null, null, 101))
        .isInstanceOf(InvalidSearchConditionException.class).hasMessage("件数は1〜100で指定してください。");

    verify(repository, never()).searchRecordListCards(any());
  }

  @Test
  void 記録の一覧_件数が1と100の場合は1件多く読むこと() {
    sut.searchRecordList(USER_ID, null, null, null, null, null, 1);
    sut.searchRecordList(USER_ID, null, null, null, null, null, 100);

    verify(repository).searchRecordListCards(argThat(query -> query.getLimit() == 2));
    verify(repository).searchRecordListCards(argThat(query -> query.getLimit() == 101));
  }

  @Test
  void 記録がある年_Repositoryが返した年をそのまま返すこと() {
    when(repository.searchRecordYears(USER_ID)).thenReturn(List.of(2026, 2025));

    List<Integer> actual = sut.searchRecordYears(USER_ID);

    assertThat(actual).containsExactly(2026, 2025);
  }
}
