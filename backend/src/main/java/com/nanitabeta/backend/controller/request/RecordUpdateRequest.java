package com.nanitabeta.backend.controller.request;

import com.nanitabeta.backend.data.FoodRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 記録の編集で受け取る内容です。
 * <p>
 * 登録と同じ項目に加えて、編集画面が読んだときの記録の版（version）を受け取ります。
 */
@Schema(description = "記録の編集の内容")
@Getter
@Setter
@NoArgsConstructor
public class RecordUpdateRequest extends RecordRequest {

  @Schema(description = "編集画面が読んだときの記録の版", example = "3",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "記録の版を指定してください。")
  @PositiveOrZero(message = "記録の版は0以上で指定してください。")
  private Integer version;

  /**
   * 編集する記録に変換します（編集画面が読んだときの版を入れる）。
   *
   * @param userId 操作するユーザーのID（JWT の sub）
   * @return 記録（IDと商品IDは未設定）
   */
  @Override
  public FoodRecord toFoodRecord(String userId) {
    FoodRecord record = super.toFoodRecord(userId);
    record.setVersion(version);
    return record;
  }

}
