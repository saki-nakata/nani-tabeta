package com.nanitabeta.backend.service;

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
import com.nanitabeta.backend.exception.InvalidSearchConditionException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.RecordRepository;
import com.nanitabeta.backend.util.PhotoPathValidator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 記録を扱うサービスです。
 */
@Service
public class RecordService {

  /** 一度に読む件数の上限 */
  private static final int MAX_LIMIT = 100;

  /** 年の絞り込みで指定できる最初の年（MySQL の DATE 型が扱える範囲） */
  private static final int MIN_YEAR = 1000;

  /** 年の絞り込みで指定できる最後の年（MySQL の DATE 型が扱える範囲） */
  private static final int MAX_YEAR = 9999;

  private RecordRepository repository;
  private ItemService itemService;
  private ShopService shopService;
  private CategoryService categoryService;

  @Autowired
  public RecordService(RecordRepository repository, ItemService itemService,
      ShopService shopService, CategoryService categoryService) {
    this.repository = repository;
    this.itemService = itemService;
    this.shopService = shopService;
    this.categoryService = categoryService;
  }

  /**
   * 記録の詳細（記録と、その商品・店・写真）を取得します。
   *
   * @param id 記録ID
   * @return 記録の詳細
   * @throws ResourceNotFoundException 記録が見つからない場合
   */
  public RecordDetail searchRecordDetail(Long id) {
    FoodRecord record = repository.searchRecord(id);
    if (record == null) {
      throw new ResourceNotFoundException("記録が見つかりません。id=" + id);
    }
    Item item = itemService.searchItem(record.getItemId());
    Shop shop = shopService.searchShop(item.getShopId());
    List<String> photoPaths = repository.searchRecordPhotoPaths(id);
    return new RecordDetail(record, item, shop, photoPaths);
  }

  /**
   * 利用者の記録の一覧を、カードの形で1回分（limit 件まで）取得します。
   * <p>
   * 並びは食べた日の新しい順、同じ日は記録IDの大きい順です。続きがある場合は、続きを読むための目印（最後のカードの食べた日と記録ID）も返します。
   * <p>
   * カードと写真は2回に分けて取得するため、同じ時点の DB を読むよう、1つのトランザクション（REPEATABLE READ）で行います。
   *
   * @param userId 利用者のID（JWT の sub）
   * @param categoryId 商品の分類ID（null の場合は絞り込まない）
   * @param shopKindId 店の業態ID（null の場合は絞り込まない）
   * @param year 食べた日の年（null の場合は絞り込まない）
   * @param cursorEatenOn 前回の最後のカードの食べた日（最初の読み込みは null）
   * @param cursorId 前回の最後のカードの記録ID（最初の読み込みは null）
   * @param limit 読む件数（1〜100）
   * @return 記録の一覧の1回分
   * @throws InvalidSearchConditionException 条件の値が正しくない場合
   */
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public RecordListPage searchRecordList(String userId, Long categoryId, Long shopKindId,
      Integer year, LocalDate cursorEatenOn, Long cursorId, int limit) {
    validateRecordListCondition(categoryId, shopKindId, year, cursorEatenOn, cursorId, limit);

    RecordListQuery query = RecordListQuery.builder().userId(userId).categoryId(categoryId)
        .shopKindId(shopKindId).eatenFrom(year == null ? null : LocalDate.of(year, 1, 1))
        .eatenTo(year == null ? null : LocalDate.of(year, 12, 31)).cursorEatenOn(cursorEatenOn)
        .cursorId(cursorId).limit(limit + 1) // 1件多く読み、続きがあるかを確かめる
        .build();
    List<RecordListCard> cards = repository.searchRecordListCards(query);

    boolean hasNext = cards.size() > limit;
    List<RecordListCard> pageCards = hasNext ? cards.subList(0, limit) : cards;
    attachPhotos(pageCards);

    RecordListCursor nextCursor = null;
    if (hasNext) {
      RecordListCard last = pageCards.getLast();
      nextCursor = new RecordListCursor(last.getEatenOn(), last.getId());
    }
    return new RecordListPage(pageCards, nextCursor);
  }

  /**
   * 利用者の記録がある年を、新しい順に取得します（年の絞り込みの選択肢に使います）。
   *
   * @param userId 利用者のID（JWT の sub）
   * @return 年の一覧（記録がない場合は空のリスト）
   */
  public List<Integer> searchRecordYears(String userId) {
    return repository.searchRecordYears(userId);
  }

  /**
   * 記録の一覧の条件の値を確認します。
   *
   * @throws InvalidSearchConditionException 条件の値が正しくない場合
   */
  private void validateRecordListCondition(Long categoryId, Long shopKindId, Integer year,
      LocalDate cursorEatenOn, Long cursorId, int limit) {
    validatePositiveId("分類ID", categoryId);
    validatePositiveId("業態ID", shopKindId);
    if (year != null && (year < MIN_YEAR || year > MAX_YEAR)) {
      throw new InvalidSearchConditionException("年は" + MIN_YEAR + "〜" + MAX_YEAR + "で指定してください。");
    }
    if ((cursorEatenOn == null && cursorId != null)
        || (cursorEatenOn != null && cursorId == null)) {
      throw new InvalidSearchConditionException("cursorEatenOn と cursorId は、両方指定するか、両方省略してください。");
    }
    validatePositiveId("cursorId", cursorId);
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new InvalidSearchConditionException("件数は1〜" + MAX_LIMIT + "で指定してください。");
    }
  }

  /**
   * ID が指定されている場合に、1以上かを確認します。
   *
   * @param name ID の名前（エラーのメッセージに使う）
   * @param id ID（null の場合は確認しない）
   * @throws InvalidSearchConditionException ID が0以下の場合
   */
  private void validatePositiveId(String name, Long id) {
    if (id != null && id <= 0) {
      throw new InvalidSearchConditionException(name + "は1以上で指定してください。");
    }
  }

  /**
   * カードに、代表写真のパスと写真の枚数を入れます。
   * <p>
   * 写真の検索に渡す記録IDは、利用者自身の一覧のカードから作ります（写真の SQL には持ち主の条件がないため）。 カードの並び順は変えません。
   *
   * @param cards 記録のカード（中身を書き換えます）
   */
  private void attachPhotos(List<RecordListCard> cards) {
    if (cards.isEmpty()) {
      return; // 空のリストを渡すと SQL の IN () がエラーになるため、写真を検索しない
    }
    List<Long> recordIdList = cards.stream().map(card -> card.getId()).toList();
    List<RecordPhoto> photos = repository.searchRecordPhotosByRecordIdList(recordIdList);
    for (RecordListCard card : cards) {
      List<RecordPhoto> cardPhotos =
          photos.stream().filter(photo -> card.getId().equals(photo.getRecordId())).toList();
      card.setPhotoCount(cardPhotos.size());
      card.setMainPhotoPath(cardPhotos.stream().min(Comparator.comparing(RecordPhoto::getSortOrder))
          .map(photo -> photo.getPhotoPath()).orElse(null));
    }
  }

  /**
   * 記録を登録します。
   * <p>
   * 同じ店に同じ商品名の商品がなければ、商品も作成します。商品の作成・記録・写真の登録は 1つのトランザクションで行い、どこかで失敗した場合はすべて取り消します。
   * <p>
   * 返す記録の詳細のうち、商品と店は取り直さず、確認・用意したときのものを使います。 同じ商品がほかのトランザクションで途中に作られた場合、普通の SELECT では
   * 最初のスナップショットの時点の DB を読むため、その商品が見えないことがあるためです。
   *
   * @param input 登録する記録（リクエストの内容。この引数は変更しない）
   * @param item 食べた商品（店ID・商品名・分類・季節限定）
   * @param photoPaths 写真のパスの一覧（先頭が代表写真。null の場合は写真なし）
   * @return 登録した記録の詳細
   */
  @Transactional
  public RecordDetail registerRecord(FoodRecord input, Item item, List<String> photoPaths) {
    Shop shop = shopService.searchShopToLink(item.getShopId()); // 存在しない場合は InvalidShopException
    categoryService.searchCategory(item.getCategoryId()); // 存在しない場合は InvalidCategoryException
    List<String> paths =
        PhotoPathValidator.validate(input.getUserId(), PhotoPathValidator.RECORDS, photoPaths);

    Item linkedItem = itemService.registerItem(item); // 同じ商品があればそれを使い、なければ作る
    FoodRecord record =
        new FoodRecord(null, input.getUserId(), linkedItem.getId(), input.getEatenOn(),
            input.getCustomNote(), input.getReview(), input.getRating(), input.getPrice());
    repository.insertRecord(record);
    if (!paths.isEmpty()) {
      repository.insertRecordPhotos(record.getId(), paths);
    }
    // 取り直すのは、自分で INSERT した記録と写真だけ。商品と店は上で手に入れたものを使う
    return new RecordDetail(repository.searchRecord(record.getId()), linkedItem, shop,
        repository.searchRecordPhotoPaths(record.getId()));
  }

  /**
   * 操作する利用者自身の記録を取得します。
   * <p>
   * 編集・削除の前に使うため、行ロック（FOR UPDATE）をかけて取得します。同じ記録への編集・削除が
   * 同時に来ても、先に取得したほうが終わるまで後のほうは待たされ、古い内容で処理しないようにするためです。
   *
   * @param id 記録ID
   * @param userId 操作する利用者のID（JWT の sub）
   * @return 記録
   * @throws ResourceNotFoundException 記録が見つからない場合
   * @throws ForbiddenException 記録が別の利用者のものである場合
   */
  private FoodRecord searchMyRecord(Long id, String userId) {
    FoodRecord record = repository.searchRecordForUpdate(id);
    if (record == null) {
      throw new ResourceNotFoundException("記録が見つかりません。id=" + id);
    }
    if (!record.getUserId().equals(userId)) {
      throw new ForbiddenException("この記録を変更する権限がありません。id=" + id);
    }
    return record;
  }

  /**
   * 記録を編集します。
   * <p>
   * 記録した本人だけが編集できます。店・商品名が変わった場合は、別の商品に付け替えます。 写真はすべて削除してから、送られた順番で入れ直します。付け替えによって前の商品の記録が
   * 0件になった場合は、前の商品を削除します。
   *
   * @param id 記録ID
   * @param input 編集後の記録（リクエストの内容。この引数は変更しない）
   * @param item 食べた商品（店ID・商品名・分類・季節限定）
   * @param photoPaths 写真のパスの一覧（先頭が代表写真。null の場合は写真なし）
   * @return 編集後の記録の詳細
   * @throws ResourceNotFoundException 記録が見つからない場合
   * @throws ForbiddenException 記録が別の利用者のものである場合
   */
  @Transactional
  public RecordDetail updateRecord(Long id, FoodRecord input, Item item, List<String> photoPaths) {
    FoodRecord myRecord = searchMyRecord(id, input.getUserId()); // 404 → 403 の順に確認
    Shop shop = shopService.searchShopToLink(item.getShopId()); // 存在しない場合は InvalidShopException
    categoryService.searchCategory(item.getCategoryId()); // 存在しない場合は InvalidCategoryException
    List<String> paths =
        PhotoPathValidator.validate(input.getUserId(), PhotoPathValidator.RECORDS, photoPaths);

    Item linkedItem = itemService.registerItem(item); // 同じ商品があればそれを使い、なければ作る
    FoodRecord record =
        new FoodRecord(id, myRecord.getUserId(), linkedItem.getId(), input.getEatenOn(),
            input.getCustomNote(), input.getReview(), input.getRating(), input.getPrice());
    repository.updateRecord(record);
    repository.deleteRecordPhotos(id);
    if (!paths.isEmpty()) {
      repository.insertRecordPhotos(id, paths);
    }
    if (!myRecord.getItemId().equals(linkedItem.getId())) {
      itemService.deleteItemIfNoRecords(myRecord.getItemId()); // 付け替えで使われなくなった前の商品
    }
    return new RecordDetail(repository.searchRecord(id), linkedItem, shop,
        repository.searchRecordPhotoPaths(id));
  }

  /**
   * 記録を削除します。
   * <p>
   * 記録した本人だけが削除できます。記録の写真・いいね・コメントも一緒に削除され、 商品の記録が0件になった場合は商品も削除します。Storage 上の写真のファイルは削除しないため、
   * 呼び出し元が返されたパスを使って削除します。
   *
   * @param id 記録ID
   * @param userId 操作する利用者のID（JWT の sub）
   * @return 削除した記録の写真のパス
   * @throws ResourceNotFoundException 記録が見つからない場合
   * @throws ForbiddenException 記録が別の利用者のものである場合
   */
  @Transactional
  public List<String> deleteRecord(Long id, String userId) {
    FoodRecord record = searchMyRecord(id, userId); // 404 → 403 の順に確認
    List<String> photoPaths = repository.searchRecordPhotoPaths(id); // 消す前に読んでおく
    repository.deleteRecord(id); // 写真・いいね・コメントも ON DELETE CASCADE で消える
    itemService.deleteItemIfNoRecords(record.getItemId());
    return photoPaths;
  }
}
