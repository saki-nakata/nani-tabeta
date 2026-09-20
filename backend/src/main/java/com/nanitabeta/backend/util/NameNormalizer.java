package com.nanitabeta.backend.util;

/**
 * 店名・商品名の表記ゆれを減らすため、文字列を整えます。
 */
public final class NameNormalizer {

  private NameNormalizer() {}

  /**
   * 名前を正規化します。
   *
   * @param name 正規化する名前
   * @return 正規化した名前
   */
  public static String normalize(String name) {
    return name.replace("　", " ").replaceAll(" +", " ").strip();
  }
}
