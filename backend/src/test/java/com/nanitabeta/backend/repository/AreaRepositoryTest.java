package com.nanitabeta.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import com.nanitabeta.backend.data.Area;
import com.nanitabeta.backend.data.Region;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AreaRepositoryTest {

  @Autowired
  private AreaRepository sut;

  @Test
  void 地域の一覧を取得できること() {
    List<Region> actual = sut.searchRegionList();

    assertThat(actual).hasSize(14);
    assertThat(actual.get(0).getRegionName()).isEqualTo("北海道・東北");
  }

  @Test
  void エリアの一覧を取得できること() {
    List<Area> actual = sut.searchAreaList();

    assertThat(actual).hasSize(81);
    assertThat(actual.get(0).getAreaName()).isEqualTo("北海道");
    assertThat(actual.get(0).getRegionId()).isEqualTo(1L);
  }
}


