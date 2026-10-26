package com.nanitabeta.backend.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 記録の一覧の1回分（カードと、続きを読むための目印）です。
 */
@Schema(description = "記録の一覧の1回分")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RecordListPage {

  @Schema(description = "記録のカード（食べた日の新しい順）", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<RecordListCard> records;

  @Schema(description = "続きを読むための目印（続きがない場合は null）")
  private RecordListCursor nextCursor;
}
