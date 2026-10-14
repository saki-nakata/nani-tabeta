import type { Shop, ShopInput } from "@/types/shop";
import { apiFetch } from "./client";

export function searchShopSuggestions(areaId: number, keyword: string) {
  const params = new URLSearchParams({ areaId: String(areaId) });
  if (keyword) {
    params.set("keyword", keyword);
  }
  return apiFetch<Shop[]>(`/api/shops/suggestions?${params}`);
}

export function registerShop(input: ShopInput) {
  return apiFetch<Shop>("/api/shops", { method: "POST", body: input });
}
