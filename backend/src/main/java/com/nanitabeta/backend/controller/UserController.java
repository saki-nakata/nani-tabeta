package com.nanitabeta.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.nanitabeta.backend.controller.request.UserCreateRequest;
import com.nanitabeta.backend.data.User;
import com.nanitabeta.backend.exception.ErrorMessage;
import com.nanitabeta.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * ユーザーを扱う REST API です。
 */
@Tag(name = "ユーザー", description = "ユーザーを扱う REST API")
@RestController
public class UserController {

  private UserService service;

  @Autowired
  public UserController(UserService service) {
    this.service = service;
  }

  /**
   * ログイン中の利用者をユーザーとして登録します。
   *
   * @param jwt ログイン中の利用者の JWT
   * @param request ユーザーの登録内容
   * @return 201 と登録したユーザー
   */
  @Operation(summary = "ユーザーの登録", description = "ログイン中の利用者をユーザーとして登録します。ユーザーIDは JWT の sub を使います。")
  @ApiResponse(responseCode = "201", description = "登録成功")
  @ApiResponse(responseCode = "400", description = "入力内容に誤りがある",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @ApiResponse(responseCode = "409", description = "すでに登録されている",
      content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
  @PostMapping("/api/users")
  public ResponseEntity<User> registerUser(@AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid UserCreateRequest request) {
    User user = service.registerUser(request.toUser(jwt.getSubject()));
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }

  /**
   * ログイン中の利用者のユーザー情報を取得します。
   *
   * @param jwt ログイン中の利用者の JWT
   * @return ユーザー
   */
  @Operation(summary = "自分の取得", description = "ログイン中の利用者のユーザー情報を取得します。")
  @ApiResponse(responseCode = "200", description = "取得成功")
  @ApiResponse(responseCode = "403", description = "ユーザーとして登録されていない")
  @GetMapping("/api/users/me")
  public User searchMe(@AuthenticationPrincipal Jwt jwt) {
    return service.searchUser(jwt.getSubject());
  }
}
