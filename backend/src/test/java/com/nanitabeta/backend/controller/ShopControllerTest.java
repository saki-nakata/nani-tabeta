package com.nanitabeta.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.ShopRegistration;
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

    verify(service).searchShop(1L);
  }

  @Test
  void 店の取得_見つからない場合は404が返ること() throws Exception {
    when(service.searchShop(999L)).thenThrow(new ResourceNotFoundException("店が見つかりません。id=999"));

    mockMvc.perform(get("/api/shops/999")).andExpect(status().isNotFound())
        .andExpect(content().json("""
            {"statusValue": 404, "statusName": "NOT_FOUND", "message": "店が見つかりません。id=999"}
            """));
  }

  @Test
  void 店の登録_新しく登録した場合は201が返ること() throws Exception {
    Shop shop = new Shop(1L, "セブンイレブン", 27L, 1L, false);
    when(service.registerShop(any())).thenReturn(new ShopRegistration(shop, true));

    mockMvc.perform(post("/api/shops").contentType(MediaType.APPLICATION_JSON).content("""
        {"shopName": "セブンイレブン", "areaId": 27, "shopKindId": 1}
        """)).andExpect(status().isCreated()).andExpect(content().json("""
        {"id": 1, "shopName": "セブンイレブン", "areaId": 27, "shopKindId": 1, "isClosed": false}
        """));
    verify(service).registerShop(any());
  }

  @Test
  void 店の登録_既存の店があった場合は200が返ること() throws Exception {
    Shop shop = new Shop(1L, "セブンイレブン", 27L, 1L, false);
    when(service.registerShop(any())).thenReturn(new ShopRegistration(shop, false));

    mockMvc.perform(post("/api/shops").contentType(MediaType.APPLICATION_JSON).content("""
        {"shopName": "セブンイレブン", "areaId": 27, "shopKindId": 1}
        """)).andExpect(status().isOk()).andExpect(content().json("""
        {"id": 1, "shopName": "セブンイレブン", "areaId": 27, "shopKindId": 1, "isClosed": false}
        """));
    verify(service).registerShop(any());
  }

  @Test
  void 店の登録_店名が空の場合は400が返ること() throws Exception {
    mockMvc.perform(post("/api/shops").contentType(MediaType.APPLICATION_JSON).content("""
        {"shopName": "", "areaId": 27, "shopKindId": 1}
        """)).andExpect(status().isBadRequest()).andExpect(content().json("""
        {"statusValue": 400, "statusName": "BAD_REQUEST", "message": "店名を入力してください。"}
        """));

    verify(service, never()).registerShop(any());
  }

}
