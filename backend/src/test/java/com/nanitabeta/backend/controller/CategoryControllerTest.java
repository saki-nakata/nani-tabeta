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
import com.nanitabeta.backend.data.Category;
import com.nanitabeta.backend.service.CategoryService;


@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CategoryService service;

  @Test
  void 分類の一覧取得_分類の一覧がJSONで返ること() throws Exception {
    when(service.searchCategoryList())
        .thenReturn(List.of(new Category(1L, "ごはんもの", "🍚"), new Category(2L, "麺類", "🍝")));

    mockMvc.perform(get("/api/categories")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().json("""
            [
              {"id": 1, "categoryName": "ごはんもの", "categoryEmoji": "🍚"},
              {"id": 2, "categoryName": "麺類", "categoryEmoji": "🍝"}
            ]
            """));

    verify(service, times(1)).searchCategoryList();
  }
}
