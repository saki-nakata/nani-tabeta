package com.nanitabeta.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nanitabeta.backend.domain.RegionDetail;
import com.nanitabeta.backend.service.AreaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 地域マスタとエリアマスタを扱う REST API です。
 */
@Tag(name = "エリアマスタ", description = "地域とエリアを扱う REST API")
@RestController
public class AreaController {

  private AreaService service;

  @Autowired
  public AreaController(AreaService service) {
    this.service = service;
  }

  /**
   * 地域ごとにエリアをまとめた一覧を取得します。
   *
   * @return 地域詳細の一覧
   */
  @Operation(summary = "エリアの一覧取得", description = "地域ごとにエリアをまとめた一覧を、地域・エリアとも id の昇順で取得します。")
  @GetMapping("/api/areas")
  public List<RegionDetail> searchRegionDetailList() {
    return service.searchRegionDetailList();
  }
}
