package com.nanitabeta.backend.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler sut = new GlobalExceptionHandler();

  @Test
  void 専用の処理がない一意制約違反は409と固定のメッセージを返すこと() {
    ResponseEntity<ErrorMessage> actual =
        sut.handleDuplicateKey(new DuplicateKeyException("Duplicate entry"));

    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(actual.getBody().getStatusValue()).isEqualTo(409);
    assertThat(actual.getBody().getStatusName()).isEqualTo("CONFLICT");
    assertThat(actual.getBody().getMessage()).isEqualTo("すでに登録されている内容と重複しています。");
  }

  @Test
  void 専用の処理がない一意制約違反はDBのメッセージを利用者に返さないこと() {
    ResponseEntity<ErrorMessage> actual = sut.handleDuplicateKey(
        new DuplicateKeyException("Duplicate entry 'セブンイレブン-20' for key 'uk_shops_name_area'"));

    assertThat(actual.getBody().getMessage()).doesNotContain("uk_shops_name_area");
  }
}
