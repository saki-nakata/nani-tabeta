package com.nanitabeta.backend.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 地域マスタです。エリアをまとめる単位（関東、東アジアなど）を表します。
 */
@Schema(description = "地域")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Region {

  @Schema(description = "地域ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(description = "地域名", example = "関東", requiredMode = Schema.RequiredMode.REQUIRED)
  private String regionName;
}
