package com.nanitabeta.backend.util;

/**
 * LIKE 検索で使う文字列を安全に扱うための処理です。
 */
public final class LikeEscaper {

  private LikeEscaper() {}

  /**
   * LIKE のワイルドカードとして解釈される文字を、ただの文字として扱えるようにします。
   * <p>
   * 置き換えの順番を変えると、あとから付けたエスケープ文字まで二重に置き換わるため、 バックスラッシュを最初に処理します。
   *
   * @param keyword キーワード
   * @return エスケープ後のキーワード
   */
  public static String escape(String keyword) {
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
