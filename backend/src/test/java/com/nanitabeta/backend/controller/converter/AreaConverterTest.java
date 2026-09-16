package com.nanitabeta.backend.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nanitabeta.backend.data.Area;
import com.nanitabeta.backend.data.Region;                // javax.swing の Region を選ばないこと
import com.nanitabeta.backend.domain.RegionDetail;


class AreaConverterTest {

    private AreaConverter sut;

    @BeforeEach
    void before() {
      sut = new AreaConverter();
    }

    @Test
    void 地域詳細の組み立て_地域ごとにエリアがまとまること() {
      Region kanto = new Region(200L, "関東");
      Region eastAsia = new Region(300L, "東アジア");
      Area tokyo = new Area(1300L, 200L, "東京都");
      Area korea = new Area(4800L, 300L, "韓国");
      Area kanagawa = new Area(1400L, 200L, "神奈川県");

      List<RegionDetail> actual = sut.mapToRegionDetailList(
          List.of(kanto, eastAsia), List.of(tokyo, korea, kanagawa));

      assertThat(actual).hasSize(2);
      assertThat(actual.get(0).getRegion()).isEqualTo(kanto);
      assertThat(actual.get(0).getAreaList()).containsExactly(tokyo, kanagawa);
      assertThat(actual.get(1).getRegion()).isEqualTo(eastAsia);
      assertThat(actual.get(1).getAreaList()).containsExactly(korea);
    }

    @Test
    void 地域詳細の組み立て_エリアのない地域は空の一覧になること() {
      Region kanto = new Region(200L, "関東");

      List<RegionDetail> actual = sut.mapToRegionDetailList(List.of(kanto), List.of());

      assertThat(actual).hasSize(1);
      assertThat(actual.get(0).getAreaList()).isEmpty();
    }
  }