package com.nanitabeta.backend.exception;

/**
 * 指定された業態が存在しないことを表す例外です。
 */
public class InvalidShopKindException extends RuntimeException {

  public InvalidShopKindException(String message) {
    super(message);
  }

  public InvalidShopKindException(String message, Throwable cause) {
    super(message, cause);
  }
}
