package com.nanitabeta.backend.exception;

/**
 * 一覧の検索の条件（絞り込み・cursor・件数）の値が正しくないことを表す例外です。
 */
public class InvalidSearchConditionException extends RuntimeException {

  public InvalidSearchConditionException(String message) {
    super(message);
  }
}
