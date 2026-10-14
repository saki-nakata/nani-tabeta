export type Shop = {
  id: number;
  shopName: string;
  areaId: number;
  shopKindId: number;
  isClosed: boolean;
};

// 店を登録するときに送る内容（POST /api/shops）
export type ShopInput = {
  shopName: string;
  areaId: number;
  shopKindId: number;
};
