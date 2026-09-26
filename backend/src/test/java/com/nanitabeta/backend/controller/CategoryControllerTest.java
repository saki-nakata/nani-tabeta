package com.nanitabeta.backend.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.nanitabeta.backend.controller.TestUsers.registeredUser;
import static com.nanitabeta.backend.controller.TestUsers.unregisteredUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nanitabeta.backend.config.SecurityConfig;
import com.nanitabeta.backend.config.UserAuthenticationConverter;
import com.nanitabeta.backend.data.Category;
import com.nanitabeta.backend.service.CategoryService;


@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CategoryService service;

  @MockitoBean
  private UserAuthenticationConverter userAuthenticationConverter; // jwt() では使われない

  @Test
  void 分類の一覧取得_分類の一覧がJSONで返ること() throws Exception {
    when(service.searchCategoryList())
        .thenReturn(List.of(new Category(1L, "ごはんもの", "🍚"), new Category(2L, "麺類", "🍝")));

    mockMvc.perform(get("/api/categories").with(registeredUser())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().json("""
            [
              {"id": 1, "categoryName": "ごはんもの", "categoryEmoji": "🍚"},
              {"id": 2, "categoryName": "麺類", "categoryEmoji": "🍝"}
            ]
            """));

    verify(service, times(1)).searchCategoryList();
  }

  @Test
  void 分類の一覧取得_JWTが付いていない場合は401が返ること() throws Exception {
    mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());

    verify(service, never()).searchCategoryList();
  }

  @Test
  void 分類の一覧取得_ユーザーとして登録されていない場合は403が返ること() throws Exception {
    mockMvc.perform(get("/api/categories").with(unregisteredUser()))
        .andExpect(status().isForbidden());

    verify(service, never()).searchCategoryList();
  }
}
