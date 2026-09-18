package com.nanitabeta.backend.controller.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.nanitabeta.backend.data.Area;
import com.nanitabeta.backend.data.Region;
import com.nanitabeta.backend.domain.RegionDetail;

/**
 * 地域とエリアを地域詳細に変換する Converter です。
 */
@Component
public class AreaConverter {

  /**
   * 地域に紐づくエリアをまとめて、地域詳細の一覧を作ります。
   *
   * @param regionList 地域の一覧
   * @param areaList エリアの一覧
   * @return 地域ごとにエリアをまとめた地域詳細の一覧
   */
  public List<RegionDetail> mapToRegionDetailList(List<Region> regionList, List<Area> areaList) {
    List<RegionDetail> regionDetailList = new ArrayList<>();
    regionList.forEach(region -> {
      List<Area> areasInRegion = areaList.stream()
          .filter(area -> region.getId().equals(area.getRegionId())).collect(Collectors.toList());
      regionDetailList.add(new RegionDetail(region, areasInRegion));
    });
    return regionDetailList;
  }
}
