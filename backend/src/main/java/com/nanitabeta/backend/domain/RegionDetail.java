package com.nanitabeta.backend.domain;

import java.util.List;

import com.nanitabeta.backend.data.Area;
import com.nanitabeta.backend.data.Region;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 地域詳細のオブジェクトです。地域と、その地域に属するエリアの一覧を持ちます。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RegionDetail {

  private Region region;
  private List<Area> areaList;

}
