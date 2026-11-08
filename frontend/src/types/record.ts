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

// 記録を削除した結果（DELETE /api/records/{id} の形）。Storage から消す写真のパスが入っている
export type RecordDeletion = {
  deletedPhotoPaths: string[];
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

// 記録の一覧の 1 マス分（GET /api/records の records の 1 件）
export type RecordListCard = {
  id: number;
  eatenOn: string;
  rating: number;
  itemName: string;
  categoryId: number;
  shopName: string;
  mainPhotoPath: string | null;
  photoCount: number;
};

// 記録の一覧の続きを読むための目印（前回の最後のカードの食べた日と記録 ID）
export type RecordListCursor = {
  eatenOn: string;
  id: number;
};

// 記録の一覧の 1 回分（GET /api/records の形）。続きがなければ nextCursor は null
export type RecordListPage = {
  records: RecordListCard[];
  nextCursor: RecordListCursor | null;
};

// 記録の一覧の絞り込み。null の項目は「すべて」（パラメータを送らない）
export type RecordListFilter = {
  categoryId: number | null;
  shopKindId: number | null;
  year: number | null;
};

// 記録の一覧の 1 マス分を、画面に出す形にしたもの（代表写真の表示用の URL を足した）
export type RecordListItem = RecordListCard & {
  mainPhotoUrl: string | null;
};

// 記録の一覧の、食べた日の範囲（"2026-11-03" の形。両方ともその日を含み、null の側は制限しない）
// 絞り込み（利用者が選ぶもの）とは別に、「今日」の欄・「これまで」の欄の区切りに使う
export type RecordListRange = {
  eatenFrom: string | null;
  eatenTo: string | null;
};
