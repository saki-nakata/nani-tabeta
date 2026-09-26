package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.jdbc.Sql;
import com.nanitabeta.backend.data.User;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

  private static final String USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";

  @Autowired
  private UserRepository sut;

  @Test
  @Sql("/sql/insert-user.sql")
  void ユーザーを1件取得できること() {
    User expected = new User(USER_ID, "Saki", null, null, "user");

    User actual = sut.searchUser(USER_ID);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void ユーザーが見つからない場合はnullを返すこと() {
    User actual = sut.searchUser("not-registered-id");

    assertThat(actual).isNull();
  }

  @Test
  void ユーザーを登録すると権限はuserになること() {
    User user = new User(USER_ID, "Saki", null, null, null);

    sut.insertUser(user);

    User actual = sut.searchUser(USER_ID);
    assertThat(actual.getNickname()).isEqualTo("Saki");
    assertThat(actual.getRole()).isEqualTo("user");
  }

  @Test
  void ユーザーの登録_権限を指定しても保存されないこと() {
    User user = new User(USER_ID, "Saki", null, null, "admin");

    sut.insertUser(user);

    User actual = sut.searchUser(USER_ID);
    assertThat(actual.getRole()).isEqualTo("user");
  }

  @Test
  @Sql("/sql/insert-user.sql")
  void ユーザーの登録_同じIDのユーザーがすでにいる場合は例外が発生すること() {
    User user = new User(USER_ID, "サキ", null, null, null);

    assertThatThrownBy(() -> sut.insertUser(user)).isInstanceOf(DuplicateKeyException.class);
  }
}
