package com.nanitabeta.backend.controller.request;

import com.nanitabeta.backend.data.Item;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 商品の更新で受け取る内容です。
 */
@Schema(description = "商品の更新内容")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemUpdateRequest {

  @Schema(description = "商品名", example = "からあげ棒", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "商品名を入力してください。")
  @Size(max = 100, message = "商品名は100文字以内で入力してください。")
  private String itemName;

  @Schema(description = "分類ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "分類を選択してください。")
  private Long categoryId;

  @Schema(description = "季節限定か", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "季節限定かどうかを指定してください。")
  private Boolean isSeasonal;

  /**
   * 更新する商品に変換します。
   *
   * @param id 商品ID（URLから受け取る）
   * @return 商品（店IDは更新しないため未設定）
   */
  public Item toItem(Long id) {
    return new Item(id, null, itemName, categoryId, isSeasonal);
  }
}
