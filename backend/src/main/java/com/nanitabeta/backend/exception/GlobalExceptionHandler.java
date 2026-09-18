package com.nanitabeta.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

/**
 * アプリケーション全体の例外を、エラー内容の JSON に変換します。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 指定されたデータが見つからない場合の例外を処理します。
   *
   * @param ex 発生した例外
   * @return HTTP 404 とエラー内容
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorMessage> handleNotFound(ResourceNotFoundException ex) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    ErrorMessage error = new ErrorMessage(status.value(), status.name(), ex.getMessage());
    return ResponseEntity.status(status).body(error);
  }

  /**
   * 想定していない例外を処理します。
   *
   * @param ex 発生した例外
   * @return HTTP 500 とエラー内容
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorMessage> handleUnexpected(Exception ex) {
    log.error("想定していない例外が発生しました", ex);
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    ErrorMessage error = new ErrorMessage(status.value(), status.name(), "サーバーでエラーが発生しました。");
    return ResponseEntity.status(status).body(error);
  }
}
