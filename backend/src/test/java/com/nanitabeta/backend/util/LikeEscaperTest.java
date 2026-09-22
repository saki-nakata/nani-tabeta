package com.nanitabeta.backend.util;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class LikeEscaperTest {

  @Test
  void パーセントをエスケープすること() {
    assertThat(LikeEscaper.escape("100%OFF")).isEqualTo("100\\%OFF");
  }

  @Test
  void アンダースコアをエスケープすること() {
    assertThat(LikeEscaper.escape("A_B")).isEqualTo("A\\_B");
  }

  @Test
  void バックスラッシュをエスケープすること() {
    assertThat(LikeEscaper.escape("A\\B")).isEqualTo("A\\\\B");
  }

  @Test
  void バックスラッシュを先に処理すること() {
    // % を先に処理すると、% に付けたバックスラッシュまで二重になってしまう
    assertThat(LikeEscaper.escape("a%b")).isEqualTo("a\\%b");
    assertThat(LikeEscaper.escape("a\\%b")).isEqualTo("a\\\\\\%b");
  }

  @Test
  void エスケープが必要な文字がない場合はそのまま返すこと() {
    assertThat(LikeEscaper.escape("からあげ棒")).isEqualTo("からあげ棒");
  }
}
