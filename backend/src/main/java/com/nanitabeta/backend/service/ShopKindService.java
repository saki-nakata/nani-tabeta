package com.nanitabeta.backend.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nanitabeta.backend.data.ShopKind;
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
}
