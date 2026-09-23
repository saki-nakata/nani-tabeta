package com.nanitabeta.backend.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.nanitabeta.backend.controller.request.ShopCreateRequest;
import com.nanitabeta.backend.controller.request.ShopUpdateRequest;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.ShopRegistration;
import com.nanitabeta.backend.exception.ErrorMessage;
import com.nanitabeta.backend.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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

  /**
   * 店を登録します。
   * <p>
   * 同じ店名とエリアの店がすでにある場合は、新しく作らずにその店を返します。
   *
   * @param request 店の登録内容
   * @return 新しく作った場合は 201、既存の店を使った場合は 200 と店
   */
  @Operation(summary = "店の登録", description = "店を登録します。同じ店名とエリアの店があれば、その店を返します。")
  @ApiResponse(responseCode = "201", description = "新しく登録した")
  @ApiResponse(responseCode = "200", description = "既存の店を使った")
  @ApiResponse(responseCode = "400", description = "入力内容に誤りがある、またはエリア・業態が存在しない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @PostMapping("/api/shops")
  public ResponseEntity<Shop> registerShop(@RequestBody @Valid ShopCreateRequest request) {
    ShopRegistration shopRegistration = service.registerShop(request.toShop());
    if (!shopRegistration.isCreated()) {
      return ResponseEntity.status(HttpStatus.OK).body(shopRegistration.getShop());
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(shopRegistration.getShop());
  }

  /**
   * 店を更新します。
   *
   * @param id 店ID
   * @param request 店の更新内容
   * @return 更新後の店
   */
  @Operation(summary = "店の更新", description = "店名・エリア・業態・閉店したかを更新します。")
  @ApiResponse(responseCode = "200", description = "更新成功")
  @ApiResponse(responseCode = "400", description = "入力内容に誤りがある、またはエリア・業態が存在しない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @ApiResponse(responseCode = "404", description = "店が見つからない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @ApiResponse(responseCode = "409", description = "同じ店名の店がそのエリアにすでにある",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @PutMapping("/api/shops/{id}")
  public Shop updateShop(@PathVariable Long id, @RequestBody @Valid ShopUpdateRequest request) {
    return service.updateShop(request.toShop(id));
  }

  /**
   * 店の登録時に表示する候補を取得します。
   * <p>
   * 閉店した店は候補に出しません。キーワードを指定した場合は、店名に含まれる店だけを返します。
   *
   * @param areaId エリアID
   * @param keyword 店名の一部（省略した場合は絞り込まない）
   * @return 店の一覧
   */
  @Operation(summary = "店の候補の取得", description = "エリア内の店を候補として取得します。閉店した店は含みません。")
  @ApiResponse(responseCode = "200", description = "取得成功")
  @ApiResponse(responseCode = "400", description = "エリアIDが指定されていない、数値でない、またはエリアが存在しない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @GetMapping("/api/shops/suggestions")
  public List<Shop> searchShopSuggestions(@RequestParam Long areaId,
      @RequestParam(required = false) String keyword) {
    return service.searchShopSuggestions(areaId, keyword);
  }
}
