package com.nanitabeta.backend.exception;

/**
 * 写真のパスが正しくないことを表す例外です。
 * <p>
 * 自分の写真の置き場所ではない、形式が違う、同じパスが重複している場合に使います。
 */
public class InvalidPhotoPathException extends RuntimeException {

  public InvalidPhotoPathException(String message) {
    super(message);
  }

  public InvalidPhotoPathException(String message, Throwable cause) {
    super(message, cause);
  }
}
