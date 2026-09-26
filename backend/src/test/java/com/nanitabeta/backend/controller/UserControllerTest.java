package com.nanitabeta.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.nanitabeta.backend.controller.TestUsers.USER_ID;
import static com.nanitabeta.backend.controller.TestUsers.registeredUser;
import static com.nanitabeta.backend.controller.TestUsers.unregisteredUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nanitabeta.backend.config.SecurityConfig;
import com.nanitabeta.backend.config.UserAuthenticationConverter;
import com.nanitabeta.backend.data.User;
import com.nanitabeta.backend.exception.DuplicateUserException;
import com.nanitabeta.backend.service.UserService;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService service;

  @MockitoBean
  private UserAuthenticationConverter userAuthenticationConverter; // jwt() では使われない

  @Test
  void ユーザーの登録_JWTのsubをIDとして登録し201が返ること() throws Exception {
    User expectedArgument = new User(USER_ID, "Saki", null, null, null);
    User expected = new User(USER_ID, "Saki", null, null, "user");
    when(service.registerUser(any())).thenReturn(expected);

    mockMvc.perform(post("/api/users").with(unregisteredUser())
        .contentType(MediaType.APPLICATION_JSON).content("""
            {"nickname": "Saki"}
            """)).andExpect(status().isCreated()).andExpect(content().json("""
            {"id": "20b622c8-dd31-46e8-8f1a-64009c7febe5", "nickname": "Saki",
             "bio": null, "profilePhotoUrl": null, "role": "user"}
            """));

    verify(service).registerUser(expectedArgument);
  }

  @Test
  void ユーザーの登録_本文で送られたIDと権限は使わないこと() throws Exception {
    User expectedArgument = new User(USER_ID, "Saki", null, null, null);
    when(service.registerUser(any())).thenReturn(new User(USER_ID, "Saki", null, null, "user"));

    mockMvc.perform(post("/api/users").with(unregisteredUser())
        .contentType(MediaType.APPLICATION_JSON).content("""
            {"nickname": "Saki", "id": "other-user-id", "role": "admin"}
            """)).andExpect(status().isCreated());

    verify(service).registerUser(expectedArgument);
  }

  @Test
  void ユーザーの登録_ニックネームが空の場合は400が返ること() throws Exception {
    mockMvc.perform(post("/api/users").with(unregisteredUser())
        .contentType(MediaType.APPLICATION_JSON).content("""
            {"nickname": ""}
            """)).andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST", "message": "ニックネームを入力してください。"}
            """));

    verify(service, never()).registerUser(any());
  }

  @Test
  void ユーザーの登録_ニックネームが30文字を超える場合は400が返ること() throws Exception {
    String nickname = "あ".repeat(31);

    mockMvc.perform(post("/api/users").with(unregisteredUser())
        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\": \"" + nickname + "\"}"))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST",
             "message": "ニックネームは30文字以内で入力してください。"}
            """));

    verify(service, never()).registerUser(any());
  }

  @Test
  void ユーザーの登録_すでに登録されている場合は409が返ること() throws Exception {
    when(service.registerUser(any())).thenThrow(new DuplicateUserException("このユーザーはすでに登録されています。"));

    mockMvc.perform(post("/api/users").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content("""
            {"nickname": "Saki"}
            """)).andExpect(status().isConflict()).andExpect(content().json("""
            {"statusValue": 409, "statusName": "CONFLICT", "message": "このユーザーはすでに登録されています。"}
            """));
  }

  @Test
  void ユーザーの登録_JWTが付いていない場合は401が返ること() throws Exception {
    mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
        {"nickname": "Saki"}
        """)).andExpect(status().isUnauthorized());

    verify(service, never()).registerUser(any());
  }

  @Test
  void 自分の取得_JWTのsubのユーザーが返ること() throws Exception {
    User expected = new User(USER_ID, "Saki", null, null, "user");
    when(service.searchUser(USER_ID)).thenReturn(expected);

    mockMvc.perform(get("/api/users/me").with(registeredUser()))
        .andExpect(status().isOk()).andExpect(content().json("""
            {"id": "20b622c8-dd31-46e8-8f1a-64009c7febe5", "nickname": "Saki", "role": "user"}
            """));

    verify(service).searchUser(USER_ID);
  }

  @Test
  void 自分の取得_ユーザーとして登録されていない場合は403が返ること() throws Exception {
    mockMvc.perform(get("/api/users/me").with(unregisteredUser()))
        .andExpect(status().isForbidden());

    verify(service, never()).searchUser(any());
  }
}
