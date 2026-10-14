import type { Item } from "@/types/item";
import { apiFetch } from "./client";

export function searchItemSuggestions(shopId: number, keyword: string) {
  const params = new URLSearchParams();
  if (keyword) {
    params.set("keyword", keyword);
  }
  return apiFetch<Item[]>(`/api/shops/${shopId}/items?${params}`);
}
