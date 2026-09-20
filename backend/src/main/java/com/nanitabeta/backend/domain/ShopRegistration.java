package com.nanitabeta.backend.domain;

import com.nanitabeta.backend.data.Shop;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 店の登録結果です。登録した店と、新しく作ったかどうかを表します。
 */
@Getter
@AllArgsConstructor
public class ShopRegistration {

  /** 店 */
  private Shop shop;

  /** 新しく作った場合は true、既存の店を使った場合は false */
  private boolean created;
}
