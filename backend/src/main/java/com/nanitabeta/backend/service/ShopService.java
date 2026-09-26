package com.nanitabeta.backend.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.ShopRegistration;
import com.nanitabeta.backend.exception.DuplicateShopNameException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.ShopRepository;
import com.nanitabeta.backend.util.LikeEscaper;
import com.nanitabeta.backend.util.NameNormalizer;

/**
 * 店を扱うサービスです。
 */
@Service
public class ShopService {

  /** 店の候補として返す最大件数 */
  private static final int SUGGESTION_LIMIT = 10;

  private ShopRepository repository;
  private AreaService areaService;
  private ShopKindService shopKindService;

  @Autowired
  public ShopService(ShopRepository repository, AreaService areaService,
      ShopKindService shopKindService) {
    this.repository = repository;
    this.areaService = areaService;
    this.shopKindService = shopKindService;
  }

  /**
   * 店を1件取得します。
   *
   * @param id 店ID
   * @return 店
   * @throws ResourceNotFoundException 店が見つからない場合
   */
  public Shop searchShop(Long id) {
    Shop shop = repository.searchShop(id);
    if (shop == null) {
      throw new ResourceNotFoundException("店が見つかりません。id=" + id);
    }
    return shop;
  }

  /**
   * 店を登録します。
   * <p>
   * 店名を正規化したうえで、同じ店名とエリアの店がすでにあれば、新しく作らずにその店を返します。 同時に同じ店が登録された場合も、あとから登録したほうは既存の店を返します。
   *
   * @param input 登録する店（リクエストの内容。この引数は変更しない）
   * @return 登録結果
   */
  @Transactional
  public ShopRegistration registerShop(Shop input) {
    areaService.searchArea(input.getAreaId()); // 存在しない場合は InvalidAreaException
    shopKindService.searchShopKind(input.getShopKindId()); // 存在しない場合は InvalidShopKindException
    Shop shop = new Shop(input.getId(), NameNormalizer.normalize(input.getShopName()),
        input.getAreaId(), input.getShopKindId(), input.getIsClosed());
    Shop matchedShop = repository.searchShopByNameAndArea(shop);
    if (matchedShop != null) {
      return new ShopRegistration(matchedShop, false);
    }
    try {
      repository.insertShop(shop);
    } catch (DuplicateKeyException ex) {
      Shop registered = repository.searchShopByNameAndAreaForShare(shop);
      if (registered == null) {
        throw ex;
      }
      return new ShopRegistration(registered, false);
    }
    Shop registerShop = repository.searchShop(shop.getId());
    return new ShopRegistration(registerShop, true);
  }

  /**
   * 店を更新します。
   * <p>
   * 店名は正規化してから保存します。変更後の店名とエリアが他の店と重複する場合は、 DuplicateShopNameException が発生します。
   *
   * @param input 更新する店（リクエストの内容。この引数は変更しない）
   * @return 更新後の店
   * @throws ResourceNotFoundException 店が見つからない場合
   * @throws DuplicateShopNameException 店名とエリアが他の店と重複する場合
   */
  @Transactional
  public Shop updateShop(Shop input) {
    searchShop(input.getId()); // 存在しない場合は ResourceNotFoundException
    areaService.searchArea(input.getAreaId()); // 存在しない場合は InvalidAreaException
    shopKindService.searchShopKind(input.getShopKindId()); // 存在しない場合は InvalidShopKindException
    Shop shop = new Shop(input.getId(), NameNormalizer.normalize(input.getShopName()),
        input.getAreaId(), input.getShopKindId(), input.getIsClosed());
    try {
      repository.updateShop(shop);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateShopNameException("同じ店名の店が、そのエリアにすでに登録されています。", ex);
    }
    return repository.searchShop(shop.getId());
  }

  /**
   * 店の登録時に表示する候補を取得します。
   * <p>
   * 閉店した店は候補に出しません。キーワードを指定した場合は、店名に含まれる店だけを返します。
   *
   * @param areaId エリアID
   * @param keyword 店名の一部（null または空文字の場合は絞り込まない）
   * @return 店の一覧（該当がない場合は空のリスト）
   */
  public List<Shop> searchShopSuggestions(Long areaId, String keyword) {
    areaService.searchArea(areaId); // 存在しない場合は InvalidAreaException
    String condition = null;
    if (keyword != null) {
      condition = LikeEscaper.escape(NameNormalizer.normalize(keyword));
    }
    return repository.searchShopSuggestions(areaId, condition, SUGGESTION_LIMIT);
  }
}
