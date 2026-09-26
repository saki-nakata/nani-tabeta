package com.nanitabeta.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nanitabeta.backend.data.User;
import com.nanitabeta.backend.exception.DuplicateUserException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.UserRepository;
import com.nanitabeta.backend.util.NameNormalizer;

/**
 * ユーザーを扱うサービスです。
 */
@Service
public class UserService {

  private UserRepository repository;

  @Autowired
  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  /**
   * ユーザーを1件取得します。
   *
   * @param id ユーザーID
   * @return ユーザー
   * @throws ResourceNotFoundException ユーザーが見つからない場合
   */
  public User searchUser(String id) {
    User user = repository.searchUser(id);
    if (user == null) {
      throw new ResourceNotFoundException("ユーザーが見つかりません。id=" + id);
    }
    return user;
  }

  /**
   * ユーザーを登録します。
   * <p>
   * ニックネームは正規化してから保存します。同じIDのユーザーがすでにいる場合は、 DuplicateUserException が発生します。
   *
   * @param input 登録するユーザー（リクエストの内容。この引数は変更しない）
   * @return 登録したユーザー
   * @throws DuplicateUserException すでに登録されている場合
   */
  @Transactional
  public User registerUser(User input) {
    User user = new User(input.getId(), NameNormalizer.normalize(input.getNickname()),
        input.getBio(), input.getProfilePhotoUrl(), input.getRole());
    try {
      repository.insertUser(user);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateUserException("このユーザーはすでに登録されています。", ex);
    }
    return repository.searchUser(user.getId());
  }
}
