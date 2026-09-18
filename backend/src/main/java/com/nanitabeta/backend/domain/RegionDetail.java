package com.nanitabeta.backend.domain;

import java.util.List;
import com.nanitabeta.backend.data.Area;
import com.nanitabeta.backend.data.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 地域詳細のオブジェクトです。地域と、その地域に属するエリアの一覧を持ちます。
 */
@Schema(description = "地域詳細（地域と、その地域に属するエリアの一覧）")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RegionDetail {

  @Schema(description = "地域", requiredMode = Schema.RequiredMode.REQUIRED)
  private Region region;

  @Schema(description = "地域に属するエリアの一覧", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<Area> areaList;

}
