package com.nanitabeta.backend.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 記録を編集した結果です。
 * <p>
 * 外した写真は、保存と同じトランザクションの中で決めたものです。Storage 上のファイルは、受け取った側が削除します。
 */
@Schema(description = "記録の編集の結果")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RecordUpdateResult {

  @Schema(description = "編集後の記録の詳細", requiredMode = Schema.RequiredMode.REQUIRED)
  private RecordDetail recordDetail;

  @Schema(description = "外した写真のパス（保存の前にあって、保存の後にない写真。Storage から削除するもの）",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<String> removedPhotoPaths;
}
