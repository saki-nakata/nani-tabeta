package com.nanitabeta.backend.exception;

/**
 * 記録の編集で、画面が見た版より新しい版がすでに保存されていることを表す例外です（HTTP 409）。
 * <p>
 * 2 つの画面で同じ記録を編集したとき、古い画面の内容で上書きしないために使います。
 */
public class RecordVersionConflictException extends RuntimeException {

  public RecordVersionConflictException(String message) {
    super(message);
  }
}
