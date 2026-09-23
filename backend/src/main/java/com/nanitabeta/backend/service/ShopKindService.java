package com.nanitabeta.backend.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nanitabeta.backend.data.ShopKind;
import com.nanitabeta.backend.exception.InvalidShopKindException;
import com.nanitabeta.backend.repository.ShopKindRepository;

/**
 * 業態マスタを扱うサービスです。
 */
@Service
public class ShopKindService {

  private ShopKindRepository repository;

  @Autowired
  public ShopKindService(ShopKindRepository repository) {
    this.repository = repository;
  }

  /**
   * 業態の一覧を取得します。
   *
   * @return 業態の一覧
   */
  public List<ShopKind> searchShopKindList() {
    return repository.searchShopKindList();
  }

  /**
   * 業態を1件取得します。
   *
   * @param id 業態ID
   * @return 業態
   * @throws InvalidShopKindException 業態が存在しない場合
   */
  public ShopKind searchShopKind(Long id) {
    ShopKind shopKind = repository.searchShopKind(id);
    if (shopKind == null) {
      throw new InvalidShopKindException("指定された業態が存在しません。shopKindId=" + id);
    }
    return shopKind;
  }
}
