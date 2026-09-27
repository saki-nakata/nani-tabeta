package com.nanitabeta.backend.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 時計の設定です。
 * <p>
 * サーバーのタイムゾーンに関係なく、日本時間で「今日」を判断するための時計を用意します。 入力チェック（{@code @PastOrPresent} など）もこの時計を使います。
 */
@Configuration
public class ClockConfig {

  /** アプリで使うタイムゾーン（日本時間） */
  public static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

  /**
   * 日本時間の時計を用意します。
   *
   * @return 日本時間の時計
   */
  @Bean
  public Clock clock() {
    return Clock.system(ZONE);
  }

  /**
   * 入力チェックが使う時計を、日本時間の時計にします。
   *
   * @param clock 日本時間の時計
   * @return 入力チェックの設定
   */
  @Bean
  public ValidationConfigurationCustomizer validationClock(Clock clock) {
    return configuration -> configuration.clockProvider(() -> clock);
  }
}
