package com.nanitabeta.backend.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 店です。
 */
@Schema(description = "店")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Shop {

  @Schema(description = "店ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(description = "店名", example = "スターバックス", requiredMode = Schema.RequiredMode.REQUIRED)
  private String shopName;

  @Schema(description = "エリアID", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long areaId;

  @Schema(description = "業態ID", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long shopKindId;

  @Schema(description = "閉店したか", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  private Boolean isClosed;
}
