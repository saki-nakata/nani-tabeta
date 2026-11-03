import { apiFetch } from "./client";
import type {
  RecordDetail,
  RecordInput,
  RecordListCursor,
  RecordListFilter,
  RecordListPage,
  RecordListRange,
} from "@/types/record";

export function registerRecord(input: RecordInput) {
  return apiFetch<RecordDetail>("/api/records", {
    method: "POST",
    body: input,
  });
}

export function searchRecord(id: number) {
  return apiFetch<RecordDetail>(`/api/records/${id}`);
}

// ログイン中の人の記録の一覧を、1 回分（20 件まで）読む
// 絞り込みが null の項目と、最初の読み込み（cursor が null）のしおりは、パラメータを送らない
export function searchRecordList(
  filter: RecordListFilter,
  range: RecordListRange,
  cursor: RecordListCursor | null,
) {
  const params = new URLSearchParams();
  if (filter.categoryId !== null) {
    params.set("categoryId", String(filter.categoryId));
  }
  if (filter.shopKindId !== null) {
    params.set("shopKindId", String(filter.shopKindId));
  }
  if (filter.year !== null) {
    params.set("year", String(filter.year));
  }
  if (range.eatenFrom !== null) {
    params.set("eatenFrom", range.eatenFrom);
  }
  if (range.eatenTo !== null) {
    params.set("eatenTo", range.eatenTo);
  }
  if (cursor) {
    params.set("cursorEatenOn", cursor.eatenOn);
    params.set("cursorId", String(cursor.id));
  }
  const query = params.toString();
  return apiFetch<RecordListPage>(
    query ? `/api/records?${query}` : "/api/records",
  );
}

// ログイン中の人の記録がある年を、新しい順に読む（年の絞り込みの選択肢に使う）
export function searchRecordYears() {
  return apiFetch<number[]>("/api/records/years");
}
