package com.nanitabeta.backend.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/**
 * Controller のテストで使う、ログイン中の利用者です。
 * <p>
 * JWT の検証と権限の決定（UserAuthenticationConverter）は行わず、それらが終わったあとの状態を直接作ります。
 */
final class TestUsers {

  /** テストで使う利用者の sub */
  static final String USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";

  private TestUsers() {}

  /**
   * users に登録済みの利用者（ROLE_USER）としてリクエストを送ります。
   *
   * @return リクエストに付けるログイン情報
   */
  static JwtRequestPostProcessor registeredUser() {
    return jwt().jwt(jwt -> jwt.subject(USER_ID))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  /**
   * ログインしているが users に未登録の利用者（権限なし）としてリクエストを送ります。
   *
   * @return リクエストに付けるログイン情報
   */
  static JwtRequestPostProcessor unregisteredUser() {
    return jwt().jwt(jwt -> jwt.subject(USER_ID)).authorities(List.of());
  }
}
