package com.nanitabeta.backend.util;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import com.nanitabeta.backend.exception.InvalidPhotoPathException;

/**
 * 写真のパスを確認するための処理です。
 * <p>
 * 写真はフロントが Supabase Storage に先にアップロードし、そのパスだけが送られてくるため、 パスが利用者自身の置き場所を指しているかを確認します。
 */
public final class PhotoPathValidator {

  /** 記録の写真のフォルダ */
  public static final String RECORDS = "records";

  /** 店の写真（メニュー・外観）のフォルダ */
  public static final String SHOPS = "shops";

  /** 写真のファイル名の形式（UUID と拡張子 .webp） */
  private static final String FILE_NAME =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp";

  private PhotoPathValidator() {}

  /**
   * 写真のパスが、利用者自身の指定したフォルダ（{userId}/{folder}/{UUID}.webp）を指しているか確認します。
   *
   * @param userId 利用者のID（JWT の sub）
   * @param folder フォルダ（{@link #RECORDS} または {@link #SHOPS}）
   * @param photoPaths 写真のパスの一覧（null の場合は写真なし）
   * @return 写真のパスの一覧（null の場合は空のリスト）
   * @throws InvalidPhotoPathException 形式が違う、他人の置き場所を指している、または重複している場合
   */
  public static List<String> validate(String userId, String folder, List<String> photoPaths) {
    if (photoPaths == null) {
      return List.of();
    }
    Pattern pattern = Pattern.compile(Pattern.quote(userId + "/" + folder + "/") + FILE_NAME);
    for (String path : photoPaths) {
      if (path == null || !pattern.matcher(path).matches()) {
        throw new InvalidPhotoPathException("写真の保存場所が正しくありません。path=" + path);
      }
    }
    if (new HashSet<>(photoPaths).size() != photoPaths.size()) {
      throw new InvalidPhotoPathException("同じ写真が重複しています。");
    }
    return photoPaths;
  }
}
