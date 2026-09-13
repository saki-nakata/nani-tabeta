package com.nanitabeta.backend.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 分類マスタです。商品の分類（スイーツ、麺類など）を表します。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Category {

  private Long id;
  private String categoryName;
  private String categoryEmoji;
}
