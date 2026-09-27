package com.nanitabeta.backend.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.nanitabeta.backend.exception.InvalidPhotoPathException;

class PhotoPathValidatorTest {

  private static final String USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";
  private static final String OTHER_USER_ID = "fa92b95a-22dd-4309-8f76-e53072d5d04c";
  private static final String UUID_1 = "0b3c9f2a-1234-4abc-9def-0123456789ab";
  private static final String UUID_2 = "7e1d2c3b-5678-4def-8abc-ba9876543210";

  @Test
  void 自分の記録のフォルダを指すパスはそのまま返すこと() {
    List<String> expected =
        List.of(USER_ID + "/records/" + UUID_1 + ".webp", USER_ID + "/records/" + UUID_2 + ".webp");

    List<String> actual =
        PhotoPathValidator.validate(USER_ID, PhotoPathValidator.RECORDS, expected);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 店の写真はshopsフォルダを指すパスが通ること() {
    List<String> expected = List.of(USER_ID + "/shops/" + UUID_1 + ".webp");

    List<String> actual = PhotoPathValidator.validate(USER_ID, PhotoPathValidator.SHOPS, expected);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void nullの場合は空のリストを返すこと() {
    List<String> actual = PhotoPathValidator.validate(USER_ID, PhotoPathValidator.RECORDS, null);

    assertThat(actual).isEmpty();
  }

  @Test
  void 他人のフォルダを指すパスは例外を投げること() {
    List<String> photoPaths = List.of(OTHER_USER_ID + "/records/" + UUID_1 + ".webp");

    assertThatThrownBy(
        () -> PhotoPathValidator.validate(USER_ID, PhotoPathValidator.RECORDS, photoPaths))
        .isInstanceOf(InvalidPhotoPathException.class)
        .hasMessage("写真の保存場所が正しくありません。path=" + photoPaths.get(0));
  }

  @Test
  void 記録の写真にshopsフォルダを指定すると例外を投げること() {
    List<String> photoPaths = List.of(USER_ID + "/shops/" + UUID_1 + ".webp");

    assertThatThrownBy(
        () -> PhotoPathValidator.validate(USER_ID, PhotoPathValidator.RECORDS, photoPaths))
        .isInstanceOf(InvalidPhotoPathException.class);
  }

  @Test
  void ファイル名がUUIDでない場合は例外を投げること() {
    List<String> photoPaths = List.of(USER_ID + "/records/photo.webp");

    assertThatThrownBy(
        () -> PhotoPathValidator.validate(USER_ID, PhotoPathValidator.RECORDS, photoPaths))
        .isInstanceOf(InvalidPhotoPathException.class);
  }

  @Test
  void 拡張子がwebpでない場合は例外を投げること() {
    List<String> photoPaths = List.of(USER_ID + "/records/" + UUID_1 + ".jpg");

    assertThatThrownBy(
        () -> PhotoPathValidator.validate(USER_ID, PhotoPathValidator.RECORDS, photoPaths))
        .isInstanceOf(InvalidPhotoPathException.class);
  }

  @Test
  void 上の階層をたどるパスは例外を投げること() {
    // 先頭は自分のフォルダだが、../ で他人のフォルダを指そうとする
    List<String> photoPaths =
        List.of(USER_ID + "/records/../../" + OTHER_USER_ID + "/records/" + UUID_1 + ".webp");

    assertThatThrownBy(
        () -> PhotoPathValidator.validate(USER_ID, PhotoPathValidator.RECORDS, photoPaths))
        .isInstanceOf(InvalidPhotoPathException.class);
  }

  @Test
  void パスにnullが含まれる場合は例外を投げること() {
    List<String> photoPaths = Arrays.asList(USER_ID + "/records/" + UUID_1 + ".webp", null);

    assertThatThrownBy(
        () -> PhotoPathValidator.validate(USER_ID, PhotoPathValidator.RECORDS, photoPaths))
        .isInstanceOf(InvalidPhotoPathException.class)
        .hasMessage("写真の保存場所が正しくありません。path=null");
  }

  @Test
  void 同じパスが重複している場合は例外を投げること() {
    String path = USER_ID + "/records/" + UUID_1 + ".webp";
    List<String> photoPaths = List.of(path, path);

    assertThatThrownBy(
        () -> PhotoPathValidator.validate(USER_ID, PhotoPathValidator.RECORDS, photoPaths))
        .isInstanceOf(InvalidPhotoPathException.class).hasMessage("同じ写真が重複しています。");
  }
}
