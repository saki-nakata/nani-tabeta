package com.nanitabeta.backend.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * エラー内容です。API がエラーを返すときの JSON の形を表します。
 */
@Schema(description = "エラー内容")
@Getter
@Setter
@AllArgsConstructor
public class ErrorMessage {

  @Schema(description = "HTTPステータスコード", example = "404")
  private int statusValue;

  @Schema(description = "HTTPステータスの名前", example = "NOT_FOUND")
  private String statusName;

  @Schema(description = "エラーメッセージ", example = "店が見つかりません。id=999")
  private String message;
}
