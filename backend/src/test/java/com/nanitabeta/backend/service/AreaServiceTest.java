package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.nanitabeta.backend.controller.converter.AreaConverter;
import com.nanitabeta.backend.data.Area;
import com.nanitabeta.backend.data.Region; // javax.swing の Region を選ばないこと
import com.nanitabeta.backend.domain.RegionDetail;
import com.nanitabeta.backend.repository.AreaRepository;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

  @Mock
  private AreaRepository repository;

  @Mock
  private AreaConverter converter;

  @InjectMocks
  private AreaService sut;

  @Test
  void 地域詳細の一覧取得_リポジトリとコンバーターが呼ばれ結果を返すこと() {
    List<Region> regionList = List.of(new Region(2L, "関東"));
    List<Area> areaList = List.of(new Area(13L, 2L, "東京都"));
    List<RegionDetail> expected = List.of(new RegionDetail(regionList.get(0), areaList));

    when(repository.searchRegionList()).thenReturn(regionList);
    when(repository.searchAreaList()).thenReturn(areaList);
    when(converter.mapToRegionDetailList(regionList, areaList)).thenReturn(expected);

    List<RegionDetail> actual = sut.searchRegionDetailList();

    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).searchRegionList();
    verify(repository, times(1)).searchAreaList();
    verify(converter, times(1)).mapToRegionDetailList(regionList, areaList);
  }
}

