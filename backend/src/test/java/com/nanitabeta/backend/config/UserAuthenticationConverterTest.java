package com.nanitabeta.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import com.nanitabeta.backend.data.User;
import com.nanitabeta.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationConverterTest {

  private static final String USER_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";

  @Mock
  private UserRepository repository;

  @InjectMocks
  private UserAuthenticationConverter sut;

  /** 検証済みの JWT（Supabase の JWT と同じく role は authenticated） */
  private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "ES256").subject(USER_ID)
      .claim("role", "authenticated").build();

  @Test
  void 一般ユーザーにはROLE_USERを付けること() {
    when(repository.searchUser(USER_ID)).thenReturn(new User(USER_ID, "Saki", null, null, "user"));

    AbstractAuthenticationToken actual = sut.convert(jwt);

    assertThat(actual.getAuthorities()).extracting(authority -> authority.getAuthority())
        .containsExactly("ROLE_USER");
    assertThat(actual.getName()).isEqualTo(USER_ID);
  }

  @Test
  void 管理者にはROLE_ADMINを付けること() {
    when(repository.searchUser(USER_ID)).thenReturn(new User(USER_ID, "Saki", null, null, "admin"));

    AbstractAuthenticationToken actual = sut.convert(jwt);

    assertThat(actual.getAuthorities()).extracting(authority -> authority.getAuthority())
        .containsExactly("ROLE_ADMIN");
  }

  @Test
  void 未登録の利用者には権限を付けないこと() {
    when(repository.searchUser(USER_ID)).thenReturn(null);

    AbstractAuthenticationToken actual = sut.convert(jwt);

    assertThat(actual.getAuthorities()).isEmpty();
    assertThat(actual.getName()).isEqualTo(USER_ID);
  }
}
