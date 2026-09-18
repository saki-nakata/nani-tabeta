package com.nanitabeta.backend.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.nanitabeta.backend.controller.converter.AreaConverter;
import com.nanitabeta.backend.data.Area;
import com.nanitabeta.backend.data.Region;
import com.nanitabeta.backend.domain.RegionDetail;
import com.nanitabeta.backend.repository.AreaRepository;

/**
 * 地域マスタとエリアマスタを扱うサービスです。
 */
@Service
public class AreaService {
  private AreaRepository repository;
  private AreaConverter converter;

  @Autowired
  public AreaService(AreaRepository repository, AreaConverter converter) {
    this.repository = repository;
    this.converter = converter;
  }

  /**
   * 地域ごとにエリアをまとめた一覧を取得します。
   *
   * @return 地域詳細の一覧
   */
  public List<RegionDetail> searchRegionDetailList() {
    List<Region> regionList = repository.searchRegionList();
    List<Area> areaList = repository.searchAreaList();
    return converter.mapToRegionDetailList(regionList, areaList);
  }

}
