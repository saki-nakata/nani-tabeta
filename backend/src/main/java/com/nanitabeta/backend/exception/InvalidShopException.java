package com.nanitabeta.backend.exception;

/**
 * リクエストの本文で指定された店が存在しないことを表す例外です。
 */
public class InvalidShopException extends RuntimeException {

  public InvalidShopException(String message) {
    super(message);
  }

  public InvalidShopException(String message, Throwable cause) {
    super(message, cause);
  }
}
