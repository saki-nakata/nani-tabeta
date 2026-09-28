package com.nanitabeta.backend.exception;

/**
 * 対象は存在するが、ログイン中の利用者には操作する権限がないことを表す例外です。
 * <p>
 * 他人の記録を編集・削除しようとした場合などに使います。
 */
public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }

  public ForbiddenException(String message, Throwable cause) {
    super(message, cause);
  }
}
