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
   * @param user 登録するユーザー
   * @return 登録したユーザー
   * @throws DuplicateUserException すでに登録されている場合
   */
  @Transactional
  public User registerUser(User user) {
    User normalizedUser = new User(user.getId(), NameNormalizer.normalize(user.getNickname()),
        user.getBio(), user.getProfilePhotoUrl(), user.getRole());
    try {
      repository.insertUser(normalizedUser);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateUserException("このユーザーはすでに登録されています。", ex);
    }
    return repository.searchUser(user.getId());
  }
}
