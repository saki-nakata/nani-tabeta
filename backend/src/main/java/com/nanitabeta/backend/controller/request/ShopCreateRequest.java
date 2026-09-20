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
 * 店の登録で受け取る内容です。
 */
@Schema(description = "店の登録内容")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopCreateRequest {

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

  /**
   * 登録する店に変換します。
   *
   * @return 店（IDと閉店したかは未設定）
   */
  public Shop toShop() {
    return new Shop(null, shopName, areaId, shopKindId, null);
  }
}

