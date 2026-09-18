package com.nanitabeta.backend.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nanitabeta.backend.data.ShopKind;
import com.nanitabeta.backend.service.ShopKindService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 業態マスタを扱う REST API です。
 */
@Tag(name = "業態マスタ", description = "店の業態を扱う REST API")
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
  @Operation(summary = "業態の一覧取得", description = "業態の一覧を id の昇順で取得します。")
  @GetMapping("/api/shop-kinds")
  public List<ShopKind> searchShopKindList() {
    return service.searchShopKindList();
  }
}
