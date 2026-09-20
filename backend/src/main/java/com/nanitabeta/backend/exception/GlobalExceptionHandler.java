package com.nanitabeta.backend.exception;

import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import lombok.extern.slf4j.Slf4j;

/**
 * アプリケーション全体の例外を、エラー内容の JSON に変換します。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 入力内容に誤りがある場合の例外を処理します。
   *
   * @param ex 発生した例外
   * @return HTTP 400 とエラー内容
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorMessage> handleValidationError(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getDefaultMessage()).collect(Collectors.joining(" "));

    HttpStatus status = HttpStatus.BAD_REQUEST;
    ErrorMessage error = new ErrorMessage(status.value(), status.name(), message);
    return ResponseEntity.status(status).body(error);
  }

  /**
   * 指定されたエリアや業態が存在しないなど、DBの制約に反する場合の例外を処理します。
   *
   * @param ex 発生した例外
   * @return HTTP 400 とエラー内容
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorMessage> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    log.warn("DBの制約に反する操作が行われました", ex);
    HttpStatus status = HttpStatus.BAD_REQUEST;
    ErrorMessage error = new ErrorMessage(status.value(), status.name(), "指定されたエリアまたは業態が存在しません。");
    return ResponseEntity.status(status).body(error);
  }

  /**
   * リクエストのパラメータが足りない場合や、型が合わない場合の例外を処理します。
   *
   * @param ex 発生した例外
   * @return HTTP 400 とエラー内容
   */
  @ExceptionHandler({MissingServletRequestParameterException.class,
      MethodArgumentTypeMismatchException.class})
  public ResponseEntity<ErrorMessage> handleInvalidParameter(Exception ex) {
    log.warn("リクエストのパラメータが正しくありません", ex);
    HttpStatus status = HttpStatus.BAD_REQUEST;
    ErrorMessage error = new ErrorMessage(status.value(), status.name(), "リクエストのパラメータが正しくありません。");
    return ResponseEntity.status(status).body(error);
  }

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
