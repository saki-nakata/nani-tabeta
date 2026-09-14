package com.nanitabeta.backend.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * エリアマスタです。店の所在地の単位（都道府県・国）を表します。
 */
@Schema(description = "エリア")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Area {

    @Schema(description = "エリアID", example = "13", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "所属する地域のID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long regionId;

    @Schema(description = "エリア名", example = "東京都", requiredMode = Schema.RequiredMode.REQUIRED)
    private String areaName;
}
