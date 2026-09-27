package com.nanitabeta.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.nanitabeta.backend.controller.request.RecordCreateRequest;
import com.nanitabeta.backend.domain.RecordDetail;
import com.nanitabeta.backend.exception.ErrorMessage;
import com.nanitabeta.backend.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 食べたものの記録を扱う REST API です。
 */
@Tag(name = "記録", description = "食べたものの記録を扱う REST API")
@RestController
public class RecordController {

  private RecordService service;

  @Autowired
  public RecordController(RecordService service) {
    this.service = service;
  }

  /**
   * 記録を1件取得します。
   *
   * @param id 記録ID
   * @return 記録の詳細
   */
  @Operation(summary = "記録の取得", description = "記録を、商品・店・写真と一緒に1件取得します。")
  @ApiResponse(responseCode = "200", description = "取得成功")
  @ApiResponse(responseCode = "404", description = "記録が見つからない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @GetMapping("/api/records/{id}")
  public RecordDetail searchRecord(@PathVariable Long id) {
    return service.searchRecordDetail(id);
  }

  /**
   * ログイン中の利用者の記録を登録します。
   *
   * @param jwt ログイン中の利用者の JWT
   * @param request 記録の登録内容
   * @return 201 と登録した記録の詳細
   */
  @Operation(summary = "記録の登録", description = "記録を登録します。同じ店に同じ商品名の商品がなければ、商品も作成します。")
  @ApiResponse(responseCode = "201", description = "登録成功")
  @ApiResponse(responseCode = "400", description = "入力内容に誤りがある、店・分類が存在しない、または写真のパスが正しくない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @PostMapping("/api/records")
  public ResponseEntity<RecordDetail> registerRecord(@AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid RecordCreateRequest request) {
    RecordDetail recordDetail = service.registerRecord(request.toFoodRecord(jwt.getSubject()),
        request.toItem(), request.getPhotoPaths());
    return ResponseEntity.status(HttpStatus.CREATED).body(recordDetail);
  }
}
