package com.nanitabeta.backend.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * エリアマスタです。店の所在地の単位（都道府県・国）を表します。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Area {
    private Long id;
    private Long regionId;
    private String areaName;
}
