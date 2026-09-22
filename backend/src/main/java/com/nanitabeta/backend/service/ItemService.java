package com.nanitabeta.backend.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.exception.DuplicateItemNameException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.ItemRepository;
import com.nanitabeta.backend.util.LikeEscaper;
import com.nanitabeta.backend.util.NameNormalizer;

/**
 * 商品を扱うサービスです。
 */
@Service
public class ItemService {

  /** 商品の候補として返す最大件数 */
  private static final int SUGGESTION_LIMIT = 10;

  private ItemRepository repository;
  private ShopService shopService;
  private CategoryService categoryService;

  @Autowired
  public ItemService(ItemRepository repository, ShopService shopService,
      CategoryService categoryService) {
    this.repository = repository;
    this.shopService = shopService;
    this.categoryService = categoryService;
  }

  /**
   * 商品を1件取得します。
   *
   * @param id 商品ID
   * @return 商品
   * @throws ResourceNotFoundException 商品が見つからない場合
   */
  public Item searchItem(Long id) {
    Item item = repository.searchItem(id);
    if (item == null) {
      throw new ResourceNotFoundException("商品が見つかりません。id=" + id);
    }
    return item;
  }

  /**
   * 指定した店の商品を、入力補助の候補として取得します。
   *
   * @param shopId 店ID
   * @param keyword 商品名の一部（null または空文字の場合は絞り込まない）
   * @return 商品の一覧（該当がない場合は空のリスト）
   * @throws ResourceNotFoundException 店が見つからない場合
   */
  public List<Item> searchItemSuggestions(Long shopId, String keyword) {
    shopService.searchShop(shopId); // 存在しない場合は ResourceNotFoundException

    String condition = null;
    if (keyword != null) {
      condition = LikeEscaper.escape(NameNormalizer.normalize(keyword));
    }
    return repository.searchItemSuggestions(shopId, condition, SUGGESTION_LIMIT);
  }

  /**
   * 商品を登録します。
   * <p>
   * 商品名を正規化したうえで、同じ店に同じ商品名の商品がすでにあれば、新しく作らずにその商品を返します。 同時に同じ商品が登録された場合も、あとから登録したほうは既存の商品を返します。
   * <p>
   * 記録の登録処理から、同じトランザクションの内側で呼び出されます。
   *
   * @param item 登録する商品
   * @return 登録した商品、または既存の商品
   */
  @Transactional
  public Item registerItem(Item item) {
    item.setItemName(NameNormalizer.normalize(item.getItemName()));
    Item matchedItem = repository.searchItemByShopAndName(item);
    if (matchedItem != null) {
      return matchedItem;
    }
    try {
      repository.insertItem(item);
    } catch (DuplicateKeyException ex) {
      Item registered = repository.searchItemByShopAndNameForShare(item);
      if (registered == null) {
        throw ex;
      }
      return registered;
    }
    return repository.searchItem(item.getId());
  }

  /**
   * 商品を更新します。
   * <p>
   * 商品名は正規化してから保存します。変更後の商品名が同じ店の他の商品と重複する場合は、 DuplicateItemNameException が発生します。
   *
   * @param item 更新する商品
   * @return 更新後の商品
   * @throws ResourceNotFoundException 商品が見つからない場合
   * @throws DuplicateItemNameException 商品名が同じ店の他の商品と重複する場合
   */
  @Transactional
  public Item updateItem(Item item) {
    searchItem(item.getId()); // 存在しない場合は ResourceNotFoundException
    categoryService.searchCategory(item.getCategoryId()); // 存在しない場合は 400

    item.setItemName(NameNormalizer.normalize(item.getItemName()));
    try {
      repository.updateItem(item);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateItemNameException("同じ商品名の商品が、その店にすでに登録されています。", ex);
    }
    return repository.searchItem(item.getId());
  }
}
