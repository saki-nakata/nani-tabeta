"use server";

import { registerShop, searchShopSuggestions } from "@/lib/api/shops";
import type { Shop, ShopInput } from "@/types/shop";
import { searchItemSuggestions } from "@/lib/api/items";
import type { Item } from "@/types/item";
import { ApiError } from "@/lib/api/client";
import { registerRecord } from "@/lib/api/records";
import type { RecordInput } from "@/types/record";

// 入力中の店名から、そのエリアの店の候補を探す（ブラウザの部品から呼ぶ）
export async function suggestShops(
  areaId: number,
  keyword: string,
): Promise<Shop[]> {
  return searchShopSuggestions(areaId, keyword);
}

// 入力中の商品名から、その店の商品の候補を探す（ブラウザの部品から呼ぶ）
export async function suggestItems(
  shopId: number,
  keyword: string,
): Promise<Item[]> {
  return searchItemSuggestions(shopId, keyword);
}

// 新しい店を登録した結果
export type RegisterNewShopResult =
  { isSuccess: true; shop: Shop } | { isSuccess: false };

// 新しい店を登録する（同じ店名・エリアの店があれば、その店が返ってくる）
export async function registerNewShop(
  input: ShopInput,
): Promise<RegisterNewShopResult> {
  try {
    const shop = await registerShop(input);
    return { isSuccess: true, shop };
  } catch (error) {
    console.error("店を登録できませんでした。", error);
    return { isSuccess: false };
  }
}

// 記録を登録した結果
// failed：確実に登録されていない（写真の削除を試みてよい）
// unknown：登録されたかわからない（写真は消さない）
export type SubmitRecordResult =
  | { status: "success"; recordId: number }
  | { status: "failed"; message: string }
  | { status: "unknown" };

// 記録を登録する（同じ店に同じ名前の商品がなければ、商品も一緒に作られる）
export async function submitRecord(
  input: RecordInput,
): Promise<SubmitRecordResult> {
  try {
    const recordDetail = await registerRecord(input);
    return { status: "success", recordId: recordDetail.record.id };
  } catch (error) {
    // バックエンドが 400 番台を返した：入力の誤りやログイン切れで、確実に登録されていない
    if (error instanceof ApiError && error.status < 500) {
      return { status: "failed", message: error.message };
    }
    // 500 番台・通信の失敗：登録されたかわからない
    console.error("記録を登録できたか確認できませんでした。", error);
    return { status: "unknown" };
  }
}
