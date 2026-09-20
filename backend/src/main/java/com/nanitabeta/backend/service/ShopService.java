package com.nanitabeta.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.ShopRegistration;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.ShopRepository;
import com.nanitabeta.backend.util.NameNormalizer;

/**
 * 店を扱うサービスです。
 */
@Service
public class ShopService {

  private ShopRepository repository;

  @Autowired
  public ShopService(ShopRepository repository) {
    this.repository = repository;
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
   * @param shop 登録する店
   * @return 登録結果
   */
  @Transactional
  public ShopRegistration registerShop(Shop shop) {
    shop.setShopName(NameNormalizer.normalize(shop.getShopName()));
    Shop shopAndArea = repository.searchShopByNameAndArea(shop);
    if (shopAndArea != null) {
      return new ShopRegistration(shopAndArea, false);
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
}
