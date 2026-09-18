package com.nanitabeta.backend.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.nanitabeta.backend.data.Area;
import com.nanitabeta.backend.data.Region;

/**
 * 地域マスタとエリアマスタを扱うリポジトリです。
 */
@Mapper
public interface AreaRepository {

  /**
   * 地域の一覧を取得します。
   *
   * @return 地域の一覧（id の昇順）
   */
  List<Region> searchRegionList();

  /**
   * エリアの一覧を取得します。
   *
   * @return エリアの一覧（id の昇順）
   */
  List<Area> searchAreaList();

}
