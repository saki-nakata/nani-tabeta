package com.nanitabeta.backend.domain;

import java.util.List;
import com.nanitabeta.backend.data.FoodRecord;
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.data.Shop;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 記録の詳細（記録と、その商品・店・写真）です。
 */
@Schema(description = "記録の詳細（記録と、その商品・店・写真）")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RecordDetail {

  @Schema(description = "記録", requiredMode = Schema.RequiredMode.REQUIRED)
  private FoodRecord record;

  @Schema(description = "食べた商品", requiredMode = Schema.RequiredMode.REQUIRED)
  private Item item;

  @Schema(description = "商品の店", requiredMode = Schema.RequiredMode.REQUIRED)
  private Shop shop;

  @Schema(description = "写真のパスの一覧（先頭が代表写真）", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<String> photoPaths;
}
