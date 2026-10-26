package com.nanitabeta.backend.controller;

import com.nanitabeta.backend.controller.request.RecordRequest;
import com.nanitabeta.backend.domain.RecordDeletion;
import com.nanitabeta.backend.domain.RecordDetail;
import com.nanitabeta.backend.domain.RecordListPage;
import com.nanitabeta.backend.exception.ErrorMessage;
import com.nanitabeta.backend.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
   * ログイン中の利用者の記録の一覧を、1回分取得します。
   *
   * @param jwt ログイン中の利用者の JWT
   * @param categoryId 商品の分類ID（省略した場合は絞り込まない）
   * @param shopKindId 店の業態ID（省略した場合は絞り込まない）
   * @param year 食べた日の年（省略した場合は絞り込まない）
   * @param cursorEatenOn 前回の nextCursor の eatenOn（最初の読み込みは省略）
   * @param cursorId 前回の nextCursor の id（最初の読み込みは省略）
   * @param limit 読む件数（省略した場合は 20）
   * @return 記録の一覧の1回分
   */
  @Operation(summary = "記録の一覧の取得",
      description = "ログイン中の利用者の記録を、食べた日の新しい順に limit 件ずつ取得します。"
          + "続きを読むときは、返された nextCursor の eatenOn と id を、cursorEatenOn と cursorId に入れて呼びます。")
  @ApiResponse(responseCode = "200", description = "取得成功（該当がない場合は空の一覧）")
  @ApiResponse(responseCode = "400", description = "条件の値が正しくない（数値・日付でない、範囲の外、cursor の片方だけなど）",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @GetMapping("/api/records")
  public RecordListPage searchRecordList(@AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long shopKindId, @RequestParam(required = false) Integer year,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cursorEatenOn,
      @RequestParam(required = false) Long cursorId, @RequestParam(defaultValue = "20") int limit) {
    return service.searchRecordList(jwt.getSubject(), categoryId, shopKindId, year, cursorEatenOn,
        cursorId, limit);
  }

  /**
   * ログイン中の利用者の記録がある年を取得します。
   *
   * @param jwt ログイン中の利用者の JWT
   * @return 年の一覧（新しい順）
   */
  @Operation(summary = "記録がある年の取得",
      description = "ログイン中の利用者の記録がある年（食べた日の年）を、新しい順に取得します。年の絞り込みの選択肢に使います。")
  @ApiResponse(responseCode = "200", description = "取得成功（記録がない場合は空のリスト）")
  @GetMapping("/api/records/years")
  public List<Integer> searchRecordYears(@AuthenticationPrincipal Jwt jwt) {
    return service.searchRecordYears(jwt.getSubject());
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
      @RequestBody @Valid RecordRequest request) {
    RecordDetail recordDetail = service.registerRecord(request.toFoodRecord(jwt.getSubject()),
        request.toItem(), request.getPhotoPaths());
    return ResponseEntity.status(HttpStatus.CREATED).body(recordDetail);
  }

  /**
   * ログイン中の利用者の記録を編集します。
   *
   * @param id 記録ID
   * @param jwt ログイン中の利用者の JWT
   * @param request 記録の編集内容
   * @return 編集後の記録の詳細
   */
  @Operation(summary = "記録の編集", description = "記録を編集します。記録した本人だけが編集できます。写真は送られた順番で入れ直します。")
  @ApiResponse(responseCode = "200", description = "編集成功")
  @ApiResponse(responseCode = "400", description = "入力内容に誤りがある、店・分類が存在しない、または写真のパスが正しくない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @ApiResponse(responseCode = "403", description = "記録が別の利用者のもの",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @ApiResponse(responseCode = "404", description = "記録が見つからない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @PutMapping("/api/records/{id}")
  public RecordDetail updateRecord(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid RecordRequest request) {
    return service.updateRecord(id, request.toFoodRecord(jwt.getSubject()), request.toItem(),
        request.getPhotoPaths());
  }

  /**
   * ログイン中の利用者の記録を削除します。
   *
   * @param id 記録ID
   * @param jwt ログイン中の利用者の JWT
   * @return 削除した記録の写真のパス
   */
  @Operation(summary = "記録の削除",
      description = "記録を削除します。記録した本人だけが削除できます。" + "返された写真のパスを使って、Storage 上のファイルを削除してください。")
  @ApiResponse(responseCode = "200", description = "削除成功")
  @ApiResponse(responseCode = "403", description = "記録が別の利用者のもの",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @ApiResponse(responseCode = "404", description = "記録が見つからない",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @DeleteMapping("/api/records/{id}")
  public RecordDeletion deleteRecord(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    List<String> deletedPhotoPaths = service.deleteRecord(id, jwt.getSubject());
    return new RecordDeletion(deletedPhotoPaths);
  }
}
