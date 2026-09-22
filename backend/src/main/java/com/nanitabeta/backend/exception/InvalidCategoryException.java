package com.nanitabeta.backend.exception;

/**
 * 指定された分類が存在しないことを表す例外です。
 */
public class InvalidCategoryException extends RuntimeException {

  public InvalidCategoryException(String message) {
    super(message);
  }

  public InvalidCategoryException(String message, Throwable cause) {
    super(message, cause);
  }
}
