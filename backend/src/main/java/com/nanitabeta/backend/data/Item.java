package com.nanitabeta.backend.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 商品です。
 */
@Schema(description = "商品")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Item {

  @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(description = "店ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long shopId;

  @Schema(description = "商品名", example = "からあげ棒", requiredMode = Schema.RequiredMode.REQUIRED)
  private String itemName;

  @Schema(description = "分類ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long categoryId;

  @Schema(description = "季節限定か", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private Boolean isSeasonal;
}
