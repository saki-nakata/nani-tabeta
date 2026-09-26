package com.nanitabeta.backend.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nanitabeta.backend.controller.CategoryController;
import com.nanitabeta.backend.data.User;
import com.nanitabeta.backend.repository.UserRepository;
import com.nanitabeta.backend.service.CategoryService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

/**
 * 本物と同じ形の JWT を送り、application.yaml の設定どおりに検証されることを確かめるテストです。
 * <p>
 * Supabase の代わりに、テストの中で鍵を作り、公開鍵（JWKS）を配る小さなサーバーを起動します。
 * SUPABASE_URL をそのサーバーに向けるので、issuer-uri・jwk-set-uri・audiences・jws-algorithms は
 * application.yaml に書いた値がそのまま使われます。
 */
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class JwtAuthenticationTest {

  private static final String USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";
  private static final String KEY_ID = "test-key";

  /** 正しい鍵（Supabase が署名に使う鍵の代わり） */
  private static final ECKey SIGNING_KEY = generateKey();

  /** 公開鍵を配るサーバー（Supabase の jwks.json の代わり） */
  private static final HttpServer JWKS_SERVER = startJwksServer();

  /** テスト中の SUPABASE_URL */
  private static final String SUPABASE_URL =
      "http://localhost:" + JWKS_SERVER.getAddress().getPort();

  private static final String ISSUER = SUPABASE_URL + "/auth/v1";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CategoryService categoryService;

  @MockitoBean
  private UserRepository userRepository;

  @DynamicPropertySource
  static void supabaseUrl(DynamicPropertyRegistry registry) {
    registry.add("SUPABASE_URL", () -> SUPABASE_URL);
  }

  @AfterAll
  static void stopJwksServer() {
    JWKS_SERVER.stop(0);
  }

  @Test
  void 正しいJWTの場合は200が返ること() throws Exception {
    when(userRepository.searchUser(USER_ID))
        .thenReturn(new User(USER_ID, "Saki", null, null, "user"));
    when(categoryService.searchCategoryList()).thenReturn(List.of());
    String token = createJwt(SIGNING_KEY, ISSUER, "authenticated", inOneHour());

    mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void 正しいJWTでもユーザーとして登録されていない場合は403が返ること() throws Exception {
    when(userRepository.searchUser(USER_ID)).thenReturn(null);
    String token = createJwt(SIGNING_KEY, ISSUER, "authenticated", inOneHour());

    mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());

    verify(categoryService, never()).searchCategoryList();
  }

  @Test
  void 別の鍵で署名されたJWTの場合は401が返ること() throws Exception {
    ECKey otherKey = generateKey(); // 同じ鍵IDだが中身が違う鍵（偽造を想定）
    String token = createJwt(otherKey, ISSUER, "authenticated", inOneHour());

    mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());

    verify(userRepository, never()).searchUser(any());
  }

  @Test
  void 発行者が違うJWTの場合は401が返ること() throws Exception {
    String otherIssuer = "https://other-project.supabase.co/auth/v1"; // 他人の Supabase プロジェクトを想定
    String token = createJwt(SIGNING_KEY, otherIssuer, "authenticated", inOneHour());

    mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());

    verify(userRepository, never()).searchUser(any());
  }

  @Test
  void 宛先が違うJWTの場合は401が返ること() throws Exception {
    String token = createJwt(SIGNING_KEY, ISSUER, "other-app", inOneHour());

    mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());

    verify(userRepository, never()).searchUser(any());
  }

  @Test
  void 有効期限が切れたJWTの場合は401が返ること() throws Exception {
    Instant expired = Instant.now().minus(5, ChronoUnit.MINUTES); // 許容される時計のずれ（60秒）より前
    String token = createJwt(SIGNING_KEY, ISSUER, "authenticated", expired);

    mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());

    verify(userRepository, never()).searchUser(any());
  }

  private static Instant inOneHour() {
    return Instant.now().plus(1, ChronoUnit.HOURS);
  }

  private static ECKey generateKey() {
    try {
      return new ECKeyGenerator(Curve.P_256).keyID(KEY_ID).generate();
    } catch (JOSEException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static HttpServer startJwksServer() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      String jwksJson = new JWKSet(SIGNING_KEY.toPublicJWK()).toString(); // 公開鍵だけを配る
      byte[] jwks = jwksJson.getBytes(StandardCharsets.UTF_8);
      server.createContext("/auth/v1/.well-known/jwks.json", exchange -> {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, jwks.length);
        try (OutputStream body = exchange.getResponseBody()) {
          body.write(jwks);
        }
      });
      server.start();
      return server;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static String createJwt(ECKey key, String issuer, String audience, Instant expiresAt)
      throws JOSEException {
    JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(USER_ID).issuer(issuer)
        .audience(audience).issueTime(new Date()).expirationTime(Date.from(expiresAt))
        .claim("role", "authenticated").build();
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID()).build();
    SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new ECDSASigner(key));
    return jwt.serialize();
  }
}
