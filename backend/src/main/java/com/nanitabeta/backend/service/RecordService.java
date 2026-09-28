package com.nanitabeta.backend.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nanitabeta.backend.data.FoodRecord;
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.RecordDetail;
import com.nanitabeta.backend.exception.ForbiddenException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.RecordRepository;
import com.nanitabeta.backend.util.PhotoPathValidator;

/**
 * 記録を扱うサービスです。
 */
@Service
public class RecordService {

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
