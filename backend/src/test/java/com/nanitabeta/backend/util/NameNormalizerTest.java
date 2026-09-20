package com.nanitabeta.backend.util;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class NameNormalizerTest {

  @Test
  void 前後の空白を取り除くこと() {
    assertThat(NameNormalizer.normalize("  セブンイレブン  ")).isEqualTo("セブンイレブン");
  }

  @Test
  void 全角スペースを半角にすること() {
    assertThat(NameNormalizer.normalize("セブン　イレブン")).isEqualTo("セブン イレブン");
  }

  @Test
  void 連続する空白を1つにすること() {
    assertThat(NameNormalizer.normalize("セブン   イレブン")).isEqualTo("セブン イレブン");
  }

  @Test
  void 全角と半角が混ざった空白を1つにすること() {
    assertThat(NameNormalizer.normalize("セブン 　 イレブン")).isEqualTo("セブン イレブン");
  }

}
