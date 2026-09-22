package com.nanitabeta.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.exception.DuplicateItemNameException;
import com.nanitabeta.backend.exception.InvalidCategoryException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.service.ItemService;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ItemService service;

  @Test
  void 商品の候補の取得_商品の一覧がJSONで返ること() throws Exception {
    List<Item> expected = List.of(new Item(1L, 1L, "からあげ棒", 4L, false));
    when(service.searchItemSuggestions(1L, "から")).thenReturn(expected);

    mockMvc.perform(get("/api/shops/1/items").param("keyword", "から"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            [{"id": 1, "shopId": 1, "itemName": "からあげ棒", "categoryId": 4, "isSeasonal": false}]
            """));

    verify(service).searchItemSuggestions(1L, "から");
  }

  @Test
  void 商品の候補の取得_キーワードを省略した場合はnullで呼ばれること() throws Exception {
    when(service.searchItemSuggestions(1L, null)).thenReturn(List.of());

    mockMvc.perform(get("/api/shops/1/items")).andExpect(status().isOk());

    verify(service).searchItemSuggestions(1L, null);
  }

  @Test
  void 商品の候補の取得_店が見つからない場合は404が返ること() throws Exception {
    when(service.searchItemSuggestions(any(), any()))
        .thenThrow(new ResourceNotFoundException("店が見つかりません。id=999"));

    mockMvc.perform(get("/api/shops/999/items")).andExpect(status().isNotFound())
        .andExpect(content().json("""
            {"statusValue": 404, "statusName": "NOT_FOUND", "message": "店が見つかりません。id=999"}
            """));
  }

  @Test
  void 商品の更新_更新後の商品がJSONで返ること() throws Exception {
    Item expectedArgument = new Item(1L, null, "からあげクン", 4L, true);
    Item expected = new Item(1L, 1L, "からあげクン", 4L, true);
    when(service.updateItem(any())).thenReturn(expected);

    mockMvc.perform(put("/api/items/1").contentType(MediaType.APPLICATION_JSON).content("""
        {"itemName": "からあげクン", "categoryId": 4, "isSeasonal": true}
        """)).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().json("""
            {"id": 1, "shopId": 1, "itemName": "からあげクン", "categoryId": 4, "isSeasonal": true}
            """));

    verify(service).updateItem(expectedArgument);
  }

  @Test
  void 商品の更新_見つからない場合は404が返ること() throws Exception {
    when(service.updateItem(any()))
        .thenThrow(new ResourceNotFoundException("商品が見つかりません。id=999"));

    mockMvc.perform(put("/api/items/999").contentType(MediaType.APPLICATION_JSON).content("""
        {"itemName": "からあげクン", "categoryId": 4, "isSeasonal": true}
        """)).andExpect(status().isNotFound()).andExpect(content().json("""
            {"statusValue": 404, "statusName": "NOT_FOUND", "message": "商品が見つかりません。id=999"}
            """));
  }

  @Test
  void 商品の更新_分類が存在しない場合は400が返ること() throws Exception {
    when(service.updateItem(any()))
        .thenThrow(new InvalidCategoryException("指定された分類が存在しません。categoryId=999"));

    mockMvc.perform(put("/api/items/1").contentType(MediaType.APPLICATION_JSON).content("""
        {"itemName": "からあげクン", "categoryId": 999, "isSeasonal": true}
        """)).andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST",
             "message": "指定された分類が存在しません。categoryId=999"}
            """));
  }

  @Test
  void 商品の更新_商品名が重複する場合は409が返ること() throws Exception {
    when(service.updateItem(any())).thenThrow(
        new DuplicateItemNameException("同じ商品名の商品が、その店にすでに登録されています。"));

    mockMvc.perform(put("/api/items/1").contentType(MediaType.APPLICATION_JSON).content("""
        {"itemName": "おにぎり 鮭", "categoryId": 1, "isSeasonal": false}
        """)).andExpect(status().isConflict()).andExpect(content().json("""
            {"statusValue": 409, "statusName": "CONFLICT",
             "message": "同じ商品名の商品が、その店にすでに登録されています。"}
            """));
  }

  @Test
  void 商品の更新_商品名が空の場合は400が返ること() throws Exception {
    mockMvc.perform(put("/api/items/1").contentType(MediaType.APPLICATION_JSON).content("""
        {"itemName": "", "categoryId": 4, "isSeasonal": true}
        """)).andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST", "message": "商品名を入力してください。"}
            """));

    verify(service, never()).updateItem(any());
  }

  @Test
  void 商品の更新_分類が未指定の場合は400が返ること() throws Exception {
    mockMvc.perform(put("/api/items/1").contentType(MediaType.APPLICATION_JSON).content("""
        {"itemName": "からあげクン", "isSeasonal": true}
        """)).andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST", "message": "分類を選択してください。"}
            """));

    verify(service, never()).updateItem(any());
  }

  @Test
  void 商品の更新_季節限定かが未指定の場合は400が返ること() throws Exception {
    mockMvc.perform(put("/api/items/1").contentType(MediaType.APPLICATION_JSON).content("""
        {"itemName": "からあげクン", "categoryId": 4}
        """)).andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST",
             "message": "季節限定かどうかを指定してください。"}
            """));

    verify(service, never()).updateItem(any());
  }
}
