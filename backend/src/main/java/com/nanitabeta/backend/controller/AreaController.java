package com.nanitabeta.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nanitabeta.backend.domain.RegionDetail;
import com.nanitabeta.backend.service.AreaService;

/**
 * 地域マスタとエリアマスタを扱う REST API です。
 */
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
    @GetMapping("/api/areas")
    public List<RegionDetail> searchRegionDetailList(){
        return service.searchRegionDetailList();
    }

}
