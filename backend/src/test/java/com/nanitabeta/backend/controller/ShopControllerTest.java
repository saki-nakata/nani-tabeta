package com.nanitabeta.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.ShopRegistration;
import com.nanitabeta.backend.exception.DuplicateShopNameException;
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
    Shop expected = new Shop(1L, "スターバックス", 20L, 9L, false);
    when(service.searchShop(1L)).thenReturn(expected);

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
    Shop expected = new Shop(1L, "セブンイレブン", 27L, 1L, false);
    when(service.registerShop(any())).thenReturn(new ShopRegistration(expected, true));

    mockMvc.perform(post("/api/shops").contentType(MediaType.APPLICATION_JSON).content("""
        {"shopName": "セブンイレブン", "areaId": 27, "shopKindId": 1}
        """)).andExpect(status().isCreated()).andExpect(content().json("""
        {"id": 1, "shopName": "セブンイレブン", "areaId": 27, "shopKindId": 1, "isClosed": false}
        """));
    verify(service).registerShop(any());
  }

  @Test
  void 店の登録_既存の店があった場合は200が返ること() throws Exception {
    Shop expected = new Shop(1L, "セブンイレブン", 27L, 1L, false);
    when(service.registerShop(any())).thenReturn(new ShopRegistration(expected, false));

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

  @Test
  void 店の候補の取得_店の一覧がJSONで返ること() throws Exception {
    when(service.searchShopSuggestions(20L, "スター"))
        .thenReturn(List.of(new Shop(1L, "スターバックス", 20L, 9L, false)));

    mockMvc.perform(get("/api/shops/suggestions").param("areaId", "20").param("keyword", "スター"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            [{"id": 1, "shopName": "スターバックス", "areaId": 20, "shopKindId": 9, "isClosed": false}]
            """));

    verify(service).searchShopSuggestions(20L, "スター");
  }

  @Test
  void 店の候補の取得_エリアIDがない場合は400が返ること() throws Exception {
    mockMvc.perform(get("/api/shops/suggestions")).andExpect(status().isBadRequest())
        .andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST",
             "message": "リクエストのパラメータが正しくありません。"}
            """));

    verify(service, never()).searchShopSuggestions(any(), any());
  }

  @Test
  void 店の更新_更新後の店がJSONで返ること() throws Exception {
    Shop expected = new Shop(1L, "スタバ", 13L, 1L, true);
    when(service.updateShop(any())).thenReturn(expected);

    mockMvc.perform(put("/api/shops/1").contentType(MediaType.APPLICATION_JSON).content("""
        {"shopName": "スタバ", "areaId": 13, "shopKindId": 1, "isClosed": true}
        """)).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().json("""
            {"id": 1, "shopName": "スタバ", "areaId": 13, "shopKindId": 1, "isClosed": true}
            """));

    verify(service).updateShop(expected);
  }

  @Test
  void 店の更新_見つからない場合は404が返ること() throws Exception {
    when(service.updateShop(any())).thenThrow(new ResourceNotFoundException("店が見つかりません。id=999"));

    mockMvc.perform(put("/api/shops/999").contentType(MediaType.APPLICATION_JSON).content("""
        {"shopName": "スタバ", "areaId": 13, "shopKindId": 1, "isClosed": true}
        """)).andExpect(status().isNotFound()).andExpect(content().json("""
        {"statusValue": 404, "statusName": "NOT_FOUND", "message": "店が見つかりません。id=999"}
        """));
  }

  @Test
  void 店の更新_店名が重複する場合は409が返ること() throws Exception {
    when(service.updateShop(any())).thenThrow(
        new DuplicateShopNameException("同じ店名の店が、そのエリアにすでに登録されています。"));

    mockMvc.perform(put("/api/shops/1").contentType(MediaType.APPLICATION_JSON).content("""
        {"shopName": "セブンイレブン", "areaId": 20, "shopKindId": 1, "isClosed": false}
        """)).andExpect(status().isConflict()).andExpect(content().json("""
        {"statusValue": 409, "statusName": "CONFLICT",
         "message": "同じ店名の店が、そのエリアにすでに登録されています。"}
        """));
  }

  @Test
  void 店の更新_閉店したかが未指定の場合は400が返ること() throws Exception {
    mockMvc.perform(put("/api/shops/1").contentType(MediaType.APPLICATION_JSON).content("""
        {"shopName": "スタバ", "areaId": 13, "shopKindId": 1}
        """)).andExpect(status().isBadRequest()).andExpect(content().json("""
        {"statusValue": 400, "statusName": "BAD_REQUEST",
         "message": "閉店したかどうかを指定してください。"}
        """));

    verify(service, never()).updateShop(any());
  }

  @Test
  void 店の取得_IDが数値でない場合は400が返ること() throws Exception {
    mockMvc.perform(get("/api/shops/abc")).andExpect(status().isBadRequest())
        .andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST",
             "message": "リクエストのパラメータが正しくありません。"}
            """));

    verify(service, never()).searchShop(any());
  }
}
