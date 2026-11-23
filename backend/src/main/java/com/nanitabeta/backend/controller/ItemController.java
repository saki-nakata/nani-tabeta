package com.nanitabeta.backend.controller;

import com.nanitabeta.backend.controller.request.ItemUpdateRequest;
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.exception.ErrorMessage;
import com.nanitabeta.backend.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品を扱う REST API です。
 */
@Tag(name = "商品", description = "商品を扱う REST API")
@RestController
public class ItemController {

  private ItemService service;

  @Autowired
  public ItemController(ItemService service) {
    this.service = service;
  }

  /**
   * 指定した店の商品を、入力補助の候補として取得します。
   * <p>
   * 記録が 1 件以上ある商品だけを返します。キーワードを指定した場合は、商品名（または読み）に当たる商品を返します。 キーワードがない場合は、ログイン中の利用者がよく使う順に並べます。
   *
   * @param jwt ログイン中の利用者の JWT
   * @param shopId 店ID
   * @param keyword 商品名の一部（省略・空文字・スペースだけの場合は絞り込まない）
   * @return 商品の一覧
   */
  @Operation(summary = "商品の候補の取得",
      description = "指定した店の商品を候補として、最大 10 件取得します。だれかの記録が 1 件以上ある商品だけを返します。keyword がある場合は、商品名または読みに当たる商品を返します。"
          + "keyword がない（省略・空文字・スペースだけ）場合は、ログイン中の利用者の記録が多い商品、同じなら最後に食べた日（eaten_on）が新しい商品の順に並べ、"
          + "自分の記録がない商品はそのうしろに商品名順（DB の並び順。読みの五十音順ではない）で並べます。")
  @ApiResponse(responseCode = "200", description = "取得成功")
  @ApiResponse(responseCode = "404", description = "店が見つからない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @GetMapping("/api/shops/{shopId}/items")
  public List<Item> searchItemSuggestions(@AuthenticationPrincipal Jwt jwt,
      @PathVariable Long shopId, @RequestParam(required = false) String keyword) {
    return service.searchItemSuggestions(jwt.getSubject(), shopId, keyword);
  }

  /**
   * 商品を更新します。
   *
   * @param id 商品ID
   * @param request 商品の更新内容
   * @return 更新後の商品
   */
  @Operation(summary = "商品の更新", description = "商品名・分類・季節限定を更新します。")
  @ApiResponse(responseCode = "200", description = "更新成功")
  @ApiResponse(responseCode = "400", description = "入力内容に誤りがある、または分類が存在しない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @ApiResponse(responseCode = "404", description = "商品が見つからない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @ApiResponse(responseCode = "409", description = "同じ商品名の商品がその店にすでにある",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @PutMapping("/api/items/{id}")
  public Item updateItem(@PathVariable Long id, @RequestBody @Valid ItemUpdateRequest request) {
    return service.updateItem(request.toItem(id));
  }
}
