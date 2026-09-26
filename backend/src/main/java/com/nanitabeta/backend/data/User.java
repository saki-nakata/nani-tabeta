package com.nanitabeta.backend.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ユーザーです。
 */
@Schema(description = "ユーザー")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class User {

  @Schema(description = "ユーザーID（Supabase Auth の sub）",
      example = "20b622c8-dd31-46e8-8f1a-64009c7febe5", requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @Schema(description = "ニックネーム", example = "Saki", requiredMode = Schema.RequiredMode.REQUIRED)
  private String nickname;

  @Schema(description = "自己紹介", example = "コンビニスイーツが好きです。")
  private String bio;

  @Schema(description = "プロフィール写真のURL")
  private String profilePhotoUrl;

  @Schema(description = "権限（user または admin）", example = "user",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String role;
}
