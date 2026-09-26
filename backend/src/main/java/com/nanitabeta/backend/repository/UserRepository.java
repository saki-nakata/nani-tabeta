package com.nanitabeta.backend.repository;

import org.apache.ibatis.annotations.Mapper;
import com.nanitabeta.backend.data.User;

/**
 * ユーザーを扱うリポジトリです。
 */
@Mapper
public interface UserRepository {

  /**
   * ユーザーを1件取得します。
   *
   * @param id ユーザーID
   * @return ユーザー（見つからない場合は null）
   */
  User searchUser(String id);

  /**
   * ユーザーを登録します。
   * <p>
   * 権限は DB の初期値（user）になります。
   *
   * @param user ユーザー
   */
  void insertUser(User user);
}
