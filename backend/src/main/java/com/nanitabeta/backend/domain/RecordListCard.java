package com.nanitabeta.backend.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 記録の一覧のカード（一覧の1件分）です。
 * <p>
 * 代表写真のパスと写真の枚数は、一覧の SQL では取らず、写真を別に取得したあとで入れます。
 */
@Schema(description = "記録の一覧のカード（一覧の1件分）")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RecordListCard {

  @Schema(description = "記録ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(description = "食べた日", example = "2026-10-24", requiredMode = Schema.RequiredMode.REQUIRED)
  private LocalDate eatenOn;

  @Schema(description = "評価（1〜5）", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer rating;

  @Schema(description = "商品名", example = "からあげ棒", requiredMode = Schema.RequiredMode.REQUIRED)
  private String itemName;

  @Schema(description = "商品の分類ID（写真がないときに分類の絵文字を出すため）", example = "4",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long categoryId;

  @Schema(description = "店名", example = "セブンイレブン", requiredMode = Schema.RequiredMode.REQUIRED)
  private String shopName;

  @Schema(description = "代表写真のパス（並び順が最初の写真。写真がない場合は null）",
      example = "20b622c8-dd31-46e8-8f1a-64009c7febe5/records/0b3c9f2a-1234-4abc-9def-0123456789ab.webp")
  private String mainPhotoPath;

  @Schema(description = "写真の枚数（写真がない場合は 0）", example = "3",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer photoCount;
}
