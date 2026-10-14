import type { Category, RegionDetail, ShopKind } from "@/types/master";
import { apiFetch } from "./client";

export function searchRegionDetails() {
  return apiFetch<RegionDetail[]>("/api/areas");
}

export function searchCategories() {
  return apiFetch<Category[]>("/api/categories");
}

export function searchShopKinds() {
  return apiFetch<ShopKind[]>("/api/shop-kinds");
}
