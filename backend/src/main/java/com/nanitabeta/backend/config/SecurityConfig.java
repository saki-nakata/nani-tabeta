package com.nanitabeta.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 認証の設定です。
 * <p>
 * Supabase Auth が発行した JWT を検証します。記録の詳細とマスタ（エリア・分類・業態）の取得は、ログインしていなくても使えます。 ユーザー登録（POST
 * /api/users）はログインしていれば使えます。それ以外の API は users に登録済みの利用者だけが使えます。
 * <p>
 * ログインしていなくても使える API でも、無効な JWT（期限切れなど）が付いていれば 401 になります。
 */
@Configuration
public class SecurityConfig {

  private UserAuthenticationConverter userAuthenticationConverter;

  @Autowired
  public SecurityConfig(UserAuthenticationConverter userAuthenticationConverter) {
    this.userAuthenticationConverter = userAuthenticationConverter;
  }

  /**
   * リクエストごとの認証のルールを定義します。
   *
   * @param http 認証のルールを組み立てる設定
   * @return 認証のルール
   * @throws Exception 設定の組み立てに失敗した場合
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll() // 誰でも
        // 記録の詳細（ID が数字のときだけ）とマスタの取得は、ログインしていなくても使える
        .requestMatchers(HttpMethod.GET, "/api/records/{id:\\d+}", "/api/areas", "/api/categories",
            "/api/shop-kinds")
        .permitAll()
        // ユーザー登録は、JWT があれば登録前でも可
        .requestMatchers(HttpMethod.POST, "/api/users").authenticated()
        // それ以外は登録済みの利用者のみ
        .anyRequest().hasAnyRole("USER", "ADMIN"))
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(userAuthenticationConverter)))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable());
    return http.build();
  }
}
