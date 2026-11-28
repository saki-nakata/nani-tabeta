import type { Category, RegionDetail, ShopKind } from "@/types/master";
import { publicApiFetch } from "./client";

// マスタはだれにでも同じ内容なので、ログインしていなくても読める publicApiFetch で読み、データだけを返す
// （ログインしていない人として読んだかどうか＝isGuest は、マスタでは使わない）

export async function searchRegionDetails() {
  return (await publicApiFetch<RegionDetail[]>("/api/areas")).data;
}

export async function searchCategories() {
  return (await publicApiFetch<Category[]>("/api/categories")).data;
}

export async function searchShopKinds() {
  return (await publicApiFetch<ShopKind[]>("/api/shop-kinds")).data;
}
