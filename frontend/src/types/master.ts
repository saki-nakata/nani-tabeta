export type Region = {
  id: number;
  regionName: string;
};

export type Area = {
  id: number;
  regionId: number;
  areaName: string;
};

// 地方と、その中のエリアの一覧（GET /api/areas の形）
export type RegionDetail = {
  region: Region;
  areaList: Area[];
};

export type Category = {
  id: number;
  categoryName: string;
  categoryEmoji: string;
};

export type ShopKind = {
  id: number;
  shopKindName: string;
  shopKindEmoji: string;
};
