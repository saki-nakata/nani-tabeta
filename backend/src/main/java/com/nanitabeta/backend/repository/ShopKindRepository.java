package com.nanitabeta.backend.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.nanitabeta.backend.data.ShopKind;

/**
 * 業態マスタを扱うリポジトリです。
 */
@Mapper
public interface ShopKindRepository {

    /**
     * 業態の一覧を取得します。
     *
     * @return 業態の一覧（id の昇順）
     */
    List<ShopKind> searchShopKindList();
}

