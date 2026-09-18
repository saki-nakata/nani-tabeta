package com.nanitabeta.backend.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.service.ShopService;

@WebMvcTest(ShopController.class)
class ShopControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ShopService service;

  @Test
  void 店の取得_店がJSONで返ること() throws Exception {
    when(service.searchShop(1L)).thenReturn(new Shop(1L, "スターバックス", 20L, 9L, false));

    mockMvc.perform(get("/api/shops/1")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().json("""
              {"id": 1, "shopName": "スターバックス", "areaId": 20, "shopKindId":9, "isClosed": false }
            """));

    verify(service, times(1)).searchShop(1L);
  }

  @Test
  void 店の取得_見つからない場合は404が返ること() throws Exception {
    when(service.searchShop(999L)).thenThrow(new ResourceNotFoundException("店が見つかりません。id=999"));

    mockMvc.perform(get("/api/shops/999")).andExpect(status().isNotFound())
        .andExpect(content().json("""
            {"statusValue": 404, "statusName": "NOT_FOUND", "message": "店が見つかりません。id=999"}
            """));
  }
}
