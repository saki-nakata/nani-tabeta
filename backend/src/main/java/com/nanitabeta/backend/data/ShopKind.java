package com.nanitabeta.backend.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 業態マスタです。店の種類（コンビニ、カフェなど）を表します。
 */
@Schema(description = "業態")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ShopKind {

    @Schema(description = "業態ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "業態名", example = "コンビニ", requiredMode = Schema.RequiredMode.REQUIRED)
    private String shopKindName;

    @Schema(description = "業態の絵文字", example = "🏪", requiredMode = Schema.RequiredMode.REQUIRED)
    private String shopKindEmoji;
}