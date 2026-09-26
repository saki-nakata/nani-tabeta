package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import com.nanitabeta.backend.data.User;
import com.nanitabeta.backend.exception.DuplicateUserException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final String USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";

  @Mock
  private UserRepository repository;

  @InjectMocks
  private UserService sut;

  @Test
  void ユーザーの取得_リポジトリの結果をそのまま返すこと() {
    User expected = new User(USER_ID, "Saki", null, null, "user");
    when(repository.searchUser(USER_ID)).thenReturn(expected);

    User actual = sut.searchUser(USER_ID);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void ユーザーの取得_見つからない場合は例外を投げること() {
    when(repository.searchUser(USER_ID)).thenReturn(null);

    assertThatThrownBy(() -> sut.searchUser(USER_ID)).isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("ユーザーが見つかりません。id=" + USER_ID);
  }

  @Test
  void ユーザーの登録_ニックネームを正規化して登録し登録後のユーザーを返すこと() {
    User user = new User(USER_ID, "　Saki　", null, null, null); // 前後に全角スペース
    User expectedArgument = new User(USER_ID, "Saki", null, null, null);
    User expected = new User(USER_ID, "Saki", null, null, "user");
    when(repository.searchUser(USER_ID)).thenReturn(expected);

    User actual = sut.registerUser(user);

    assertThat(actual).isEqualTo(expected);
    verify(repository).insertUser(expectedArgument); // 正規化した名前で保存を依頼すること
  }

  @Test
  void ユーザーの登録_すでに登録されている場合はユーザー専用の例外に変換すること() {
    User user = new User(USER_ID, "Saki", null, null, null);
    doThrow(new DuplicateKeyException("重複")).when(repository).insertUser(any());

    assertThatThrownBy(() -> sut.registerUser(user)).isInstanceOf(DuplicateUserException.class)
        .hasMessage("このユーザーはすでに登録されています。").hasCauseInstanceOf(DuplicateKeyException.class);
  }
}
