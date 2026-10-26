package com.nanitabeta.backend.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 記録の写真（record_photos の1行）です。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RecordPhoto {

  /** 記録ID */
  private Long recordId;

  /** 写真のパス */
  private String photoPath;

  /** 並び順（0 が代表写真） */
  private Integer sortOrder;
}
