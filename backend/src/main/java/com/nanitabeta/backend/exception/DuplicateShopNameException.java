package com.nanitabeta.backend.exception;

/**
 * 店名とエリアの組み合わせが、すでに登録されている店と重複していることを表す例外です。
 */
public class DuplicateShopNameException extends RuntimeException {

  public DuplicateShopNameException(String message) {
    super(message);
  }

  public DuplicateShopNameException(String message, Throwable cause) {
    super(message, cause);
  }
}
