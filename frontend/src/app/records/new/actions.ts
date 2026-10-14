"use server";

import { searchShopSuggestions } from "@/lib/api/shops";
import type { Shop } from "@/types/shop";
import { searchItemSuggestions } from "@/lib/api/items";
import type { Item } from "@/types/item";

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
