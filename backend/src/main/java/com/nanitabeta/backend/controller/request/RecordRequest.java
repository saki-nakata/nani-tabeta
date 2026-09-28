package com.nanitabeta.backend.controller.request;

import java.time.LocalDate;
import java.util.List;
import com.nanitabeta.backend.data.FoodRecord;
import com.nanitabeta.backend.data.Item;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 記録の登録・編集で受け取る内容です。
 * <p>
 * 記録したユーザーは JWT の sub から決めるため、ここでは受け取りません。
 */
@Schema(description = "記録の登録・編集の内容")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordRequest {

  @Schema(description = "店ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "店を選択してください。")
  private Long shopId;

  @Schema(description = "商品名", example = "からあげ棒", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "商品名を入力してください。")
  @Size(max = 100, message = "商品名は100文字以内で入力してください。")
  private String itemName;

  @Schema(description = "分類ID（商品を新しく作るときに使う）", example = "4",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "分類を選択してください。")
  private Long categoryId;

  @Schema(description = "季節限定か（商品を新しく作るときに使う）", example = "false",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "季節限定かどうかを指定してください。")
  private Boolean isSeasonal;

  @Schema(description = "食べた日", example = "2026-09-26", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "食べた日を入力してください。")
  @PastOrPresent(message = "食べた日に未来の日付は指定できません。")
  private LocalDate eatenOn;

  @Schema(description = "評価（1〜5）", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "評価を選択してください。")
  @Min(value = 1, message = "評価は1〜5で選択してください。")
  @Max(value = 5, message = "評価は1〜5で選択してください。")
  private Integer rating;

  @Schema(description = "感想", example = "衣がサクサクでおいしかった")
  @Size(max = 1000, message = "感想は1000文字以内で入力してください。")
  private String review;

  @Schema(description = "カスタム内容", example = "氷少なめ")
  @Size(max = 500, message = "カスタム内容は500文字以内で入力してください。")
  private String customNote;

  @Schema(description = "価格（円）", example = "220")
  @PositiveOrZero(message = "価格は0以上で入力してください。")
  private Integer price;

  @Schema(description = "写真のパスの一覧（先頭が代表写真。5枚まで）",
      example = "[\"20b622c8-dd31-46e8-8f1a-64009c7febe5/records/0b3c….webp\"]")
  @Size(max = 5, message = "写真は5枚まで登録できます。")
  private List<String> photoPaths;

  /**
   * 登録・編集する記録に変換します。
   *
   * @param userId 操作するユーザーのID（JWT の sub）
   * @return 記録（IDと商品IDは未設定）
   */
  public FoodRecord toFoodRecord(String userId) {
    return new FoodRecord(null, userId, null, eatenOn, customNote, review, rating, price);
  }

  /**
   * 記録する商品に変換します。
   * <p>
   * 同じ店に同じ商品名の商品がすでにあれば、その商品が使われます（分類・季節限定は変更しません）。
   *
   * @return 商品（IDは未設定）
   */
  public Item toItem() {
    return new Item(null, shopId, itemName, categoryId, isSeasonal);
  }
}
