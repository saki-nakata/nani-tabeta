import type { RecordListFilter } from "@/types/record";

// 絞り込みなし（分類・業態・年が、全部「すべて」）
export const ALL_FILTER: RecordListFilter = {
  categoryId: null,
  shopKindId: null,
  year: null,
};

// 分類・業態・年のどれか 1 つでも絞り込んでいるか（0 件のときの文を分けるため）
export function isFiltered(filter: RecordListFilter): boolean {
  return (
    filter.categoryId !== null ||
    filter.shopKindId !== null ||
    filter.year !== null
  );
}
