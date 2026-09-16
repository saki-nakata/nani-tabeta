package com.nanitabeta.backend.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nanitabeta.backend.data.Area;
import com.nanitabeta.backend.data.Region;                // javax.swing の Region を選ばないこと
import com.nanitabeta.backend.domain.RegionDetail;
import com.nanitabeta.backend.service.AreaService;


@WebMvcTest(AreaController.class)
class AreaControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AreaService service;

  @Test
  void エリアの一覧取得_地域ごとにエリアをまとめたJSONが返ること() throws Exception {
    Region kanto = new Region(2L, "関東");
    List<Area> areaList = List.of(
        new Area(13L, 2L, "東京都"),
        new Area(14L, 2L, "神奈川県"));
    when(service.searchRegionDetailList()).thenReturn(List.of(new RegionDetail(kanto, areaList)));

    mockMvc.perform(get("/api/areas"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            [
              {
                "region": {"id": 2, "regionName": "関東"},
                "areaList": [
                  {"id": 13, "regionId": 2, "areaName": "東京都"},
                  {"id": 14, "regionId": 2, "areaName": "神奈川県"}
                ]
              }
            ]
            """));

    verify(service, times(1)).searchRegionDetailList();
  }
}

