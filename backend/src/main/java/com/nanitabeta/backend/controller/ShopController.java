package com.nanitabeta.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.exception.ErrorMessage;
import com.nanitabeta.backend.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 店を扱う REST API です。
 */
@Tag(name = "店", description = "店を扱う REST API")
@RestController
public class ShopController {

  private ShopService service;

  @Autowired
  public ShopController(ShopService service) {
    this.service = service;
  }

  /**
   * 店を1件取得します。
   *
   * @param id 店ID
   * @return 店
   */
  @Operation(summary = "店の取得", description = "店を1件取得します。")
  @ApiResponse(responseCode = "200", description = "取得成功")
  @ApiResponse(responseCode = "404", description = "店が見つからない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @GetMapping("/api/shops/{id}")
  public Shop searchShop(@PathVariable Long id) {
    return service.searchShop(id);
  }
}
