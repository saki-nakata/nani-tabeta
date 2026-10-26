package com.nanitabeta.backend.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 記録の一覧の続きを読むための目印（前回の最後のカードの食べた日と記録ID）です。
 */
@Schema(description = "記録の一覧の続きを読むための目印（前回の最後のカードの食べた日と記録ID）")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RecordListCursor {

  @Schema(description = "最後のカードの食べた日（次のリクエストの cursorEatenOn に入れる）", example = "2026-10-20",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private LocalDate eatenOn;

  @Schema(description = "最後のカードの記録ID（次のリクエストの cursorId に入れる）", example = "9",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;
}
