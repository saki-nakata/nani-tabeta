package com.nanitabeta.backend.data;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 食べたものの記録です。
 */
@Schema(description = "記録")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FoodRecord {

  @Schema(description = "記録ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(description = "記録したユーザーのID（Supabase Auth の sub）",
      example = "20b622c8-dd31-46e8-8f1a-64009c7febe5", requiredMode = Schema.RequiredMode.REQUIRED)
  private String userId;

  @Schema(description = "商品ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long itemId;

  @Schema(description = "食べた日", example = "2026-09-26", requiredMode = Schema.RequiredMode.REQUIRED)
  private LocalDate eatenOn;

  @Schema(description = "カスタム内容（次回そのまま注文するためのメモ）", example = "氷少なめ")
  private String customNote;

  @Schema(description = "感想", example = "衣がサクサクでおいしかった")
  private String review;

  @Schema(description = "評価（1〜5）", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer rating;

  @Schema(description = "価格（円）", example = "220")
  private Integer price;
}
