package com.nanitabeta.backend.controller.request;

import com.nanitabeta.backend.data.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ユーザーの登録で受け取る内容です。
 * <p>
 * ユーザーIDは JWT の sub から決め、権限は受け取りません。
 */
@Schema(description = "ユーザーの登録内容")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

  @Schema(description = "ニックネーム", example = "Saki", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "ニックネームを入力してください。")
  @Size(max = 30, message = "ニックネームは30文字以内で入力してください。")
  private String nickname;

  /**
   * 登録するユーザーに変換します。
   *
   * @param id ユーザーID（JWT の sub）
   * @return ユーザー（自己紹介・プロフィール写真・権限は未設定）
   */
  public User toUser(String id) {
    return new User(id, nickname, null, null, null);
  }
}
