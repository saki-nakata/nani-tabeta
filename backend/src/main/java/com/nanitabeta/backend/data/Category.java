package com.nanitabeta.backend.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 分類マスタです。商品の分類（スイーツ、麺類など）を表します。
 */
@Schema(description = "分類")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Category {

  @Schema(description = "分類ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(description = "分類名", example = "ごはんもの", requiredMode = Schema.RequiredMode.REQUIRED)
  private String categoryName;

  @Schema(description = "分類の絵文字", example = "🍚", requiredMode = Schema.RequiredMode.REQUIRED)
  private String categoryEmoji;

}
