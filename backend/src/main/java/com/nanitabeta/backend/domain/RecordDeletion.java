package com.nanitabeta.backend.domain;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 記録を削除した結果です。
 * <p>
 * DB から消した記録の写真のパスを返し、Storage 上のファイルはフロントが削除します。
 */
@Schema(description = "記録の削除結果")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecordDeletion {

  @Schema(description = "削除した記録の写真のパス（Storage から削除するもの）",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<String> deletedPhotoPaths;
}
