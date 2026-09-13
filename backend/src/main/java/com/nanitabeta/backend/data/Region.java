package com.nanitabeta.backend.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 地域マスタです。エリアをまとめる単位（関東、東アジアなど）を表します。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Region {
    private Long id;
    private String regionName;
}
