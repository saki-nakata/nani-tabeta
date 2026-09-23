package com.nanitabeta.backend.exception;

/**
 * 指定されたエリアが存在しないことを表す例外です。
 */
public class InvalidAreaException extends RuntimeException {

  public InvalidAreaException(String message) {
    super(message);
  }

  public InvalidAreaException(String message, Throwable cause) {
    super(message, cause);
  }
}
