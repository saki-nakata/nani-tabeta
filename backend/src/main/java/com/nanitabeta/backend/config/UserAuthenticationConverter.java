package com.nanitabeta.backend.config;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import com.nanitabeta.backend.data.User;
import com.nanitabeta.backend.repository.UserRepository;

/**
 * 検証済みの JWT から、ログイン中の利用者の権限を決めます。
 * <p>
 * JWT の sub で users を探し、登録済みなら users.role に応じて ROLE_USER または ROLE_ADMIN を付けます。
 * 未登録の場合は権限を付けません。Supabase の JWT に含まれる role は使いません。
 */
@Component
public class UserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private UserRepository repository;

  @Autowired
  public UserAuthenticationConverter(UserRepository repository) {
    this.repository = repository;
  }

  /**
   * JWT を、権限付きのログイン情報に変換します。
   *
   * @param jwt 検証済みの JWT
   * @return ログイン情報（未登録の場合は権限なし）
   */
  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    User user = repository.searchUser(jwt.getSubject());
    if (user == null) {
      return new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject()); // 未登録：権限なし
    }
    String role = "ROLE_" + user.getRole().toUpperCase(Locale.ROOT); // user → ROLE_USER
    return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role)),
        jwt.getSubject());
  }
}
