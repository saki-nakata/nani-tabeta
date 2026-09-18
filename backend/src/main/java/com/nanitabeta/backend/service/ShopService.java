package com.nanitabeta.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.ShopRepository;

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
}
