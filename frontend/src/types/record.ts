import type { Item } from "@/types/item";
import type { Shop } from "@/types/shop";

// 記録（バックエンドの FoodRecord の形）
export type FoodRecord = {
  id: number;
  userId: string;
  itemId: number;
  eatenOn: string;
  customNote: string | null;
  review: string | null;
  rating: number;
  price: number | null;
};

// 記録の詳細（記録と、その商品・店・写真）。登録したときと、1 件取得したときに返ってくる
export type RecordDetail = {
  record: FoodRecord;
  item: Item;
  shop: Shop;
  photoPaths: string[];
};

// 記録を登録するときに送る内容（POST /api/records）
// 商品は ID ではなく商品名で送る。同じ店に同じ名前の商品がなければ、バックエンドが作る
export type RecordInput = {
  shopId: number;
  itemName: string;
  categoryId: number;
  isSeasonal: boolean;
  eatenOn: string;
  rating: number;
  review: string | null;
  customNote: string | null;
  price: number | null;
  photoPaths: string[];
};
