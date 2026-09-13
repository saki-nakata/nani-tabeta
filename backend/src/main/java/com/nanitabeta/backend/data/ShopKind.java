package com.nanitabeta.backend.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 業態マスタです。店の種類（コンビニ、カフェなど）を表します。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ShopKind {
    private Long id;
    private String shopKindName;
    private String shopKindEmoji;
}