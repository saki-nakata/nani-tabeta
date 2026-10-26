package com.nanitabeta.backend.domain;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * 記録の一覧を検索するときの条件（絞り込み・cursor・件数）です。
 * <p>
 * null の項目は、その条件で絞り込みません（userId と limit は必ず指定します）。
 */
@Getter
@Builder
public class RecordListQuery {

  /** 記録した人（ログイン中の利用者）のID */
  private String userId;

  /** 商品の分類ID */
  private Long categoryId;

  /** 店の業態ID */
  private Long shopKindId;

  /** 食べた日の範囲の最初の日（この日を含む） */
  private LocalDate eatenFrom;

  /** 食べた日の範囲の最後の日（この日を含む） */
  private LocalDate eatenTo;

  /** cursor：前のページの最後の記録の食べた日（この記録より後ろを読む） */
  private LocalDate cursorEatenOn;

  /** cursor：前のページの最後の記録のID */
  private Long cursorId;

  /** 読む最大件数 */
  private int limit;
}
