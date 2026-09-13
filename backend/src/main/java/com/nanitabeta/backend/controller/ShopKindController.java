package com.nanitabeta.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nanitabeta.backend.data.ShopKind;
import com.nanitabeta.backend.service.ShopKindService;

/**
 * 業態マスタを扱う REST API です。
 */
@RestController
public class ShopKindController {

  private ShopKindService service;

  @Autowired
  public ShopKindController(ShopKindService service) {
    this.service = service;
  }

  /**
   * 業態の一覧を取得します。
   *
   * @return 業態の一覧
   */
  @GetMapping("/api/shop-kinds")
  public List<ShopKind> searchShopKindList() {
    return service.searchShopKindList();
  }
}