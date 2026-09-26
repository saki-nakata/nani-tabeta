package com.nanitabeta.backend.exception;

/**
 * ログイン中の利用者が、すでにユーザーとして登録されていることを表す例外です。
 */
public class DuplicateUserException extends RuntimeException {

  public DuplicateUserException(String message) {
    super(message);
  }

  public DuplicateUserException(String message, Throwable cause) {
    super(message, cause);
  }
}
