package com.nanitabeta.backend.exception;

/**
 * 指定されたデータが見つからないことを表す例外です。
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}

