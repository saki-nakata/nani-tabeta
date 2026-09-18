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
import com.nanitabeta.backend.data.ShopKind;
import com.nanitabeta.backend.service.ShopKindService;


@WebMvcTest(ShopKindController.class)
class ShopKindControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ShopKindService service;

  @Test
  void 業態の一覧取得_業態の一覧がJSONで返ること() throws Exception {
    when(service.searchShopKindList())
        .thenReturn(List.of(new ShopKind(1L, "コンビニ", "🏪"), new ShopKind(2L, "スーパー・食品売場", "🛒")));

    mockMvc.perform(get("/api/shop-kinds")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().json("""
            [
              {"id": 1, "shopKindName": "コンビニ", "shopKindEmoji": "🏪"},
              {"id": 2, "shopKindName": "スーパー・食品売場", "shopKindEmoji": "🛒"}
            ]
            """));

    verify(service, times(1)).searchShopKindList();
  }
}
