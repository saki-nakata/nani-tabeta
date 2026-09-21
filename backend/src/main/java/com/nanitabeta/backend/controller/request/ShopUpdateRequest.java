package com.nanitabeta.backend.controller.request;

import com.nanitabeta.backend.data.Shop;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 店の更新で受け取る内容です。
 */
@Schema(description = "店の更新内容")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopUpdateRequest {

  @Schema(description = "店名", example = "セブンイレブン", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "店名を入力してください。")
  @Size(max = 100, message = "店名は100文字以内で入力してください。")
  private String shopName;

  @Schema(description = "エリアID", example = "27", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "エリアを選択してください。")
  private Long areaId;

  @Schema(description = "業態ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "業態を選択してください。")
  private Long shopKindId;

  @Schema(description = "閉店したか", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "閉店したかどうかを指定してください。")
  private Boolean isClosed;

  /**
   * 更新する店に変換します。
   *
   * @param id 店ID
   * @return 店
   */
  public Shop toShop(Long id) {
    return new Shop(id, shopName, areaId, shopKindId, isClosed);
  }
}
