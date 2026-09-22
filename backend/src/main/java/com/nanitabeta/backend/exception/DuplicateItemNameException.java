package com.nanitabeta.backend.exception;

/**
 * 店と商品名の組み合わせが、すでに登録されている商品と重複していることを表す例外です。
 */
public class DuplicateItemNameException extends RuntimeException {

  public DuplicateItemNameException(String message) {
    super(message);
  }

  public DuplicateItemNameException(String message, Throwable cause) {
    super(message, cause);
  }
}
