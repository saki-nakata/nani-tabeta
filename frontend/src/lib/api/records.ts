import { ApiError, apiFetch } from "./client";
import type {
  RecordDetail,
  RecordInput,
  RecordListCursor,
  RecordListFilter,
  RecordListPage,
  RecordListRange,
  RecordDeletion,
  RecordUpdateResult,
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

// 記録を取りに行く。見つからない（404）ときだけ null を返し、それ以外の失敗はそのまま投げる（エラーの画面になる）
export async function findRecord(id: number) {
  try {
    return await searchRecord(id);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

// 記録を編集する（本人の記録だけ）。写真は、残す写真と新しい写真を、並んだ順に全部送る
// version は、編集画面が読んだときの記録の版。ほかの画面で先に保存されていれば 409 になる
export function updateRecord(id: number, input: RecordInput, version: number) {
  return apiFetch<RecordUpdateResult>(`/api/records/${id}`, {
    method: "PUT",
    body: { ...input, version },
  });
}

// 記録を削除する（本人の記録だけ）。消した記録の写真のパスが返ってくる
export async function deleteRecord(id: number): Promise<string[]> {
  const deletion = await apiFetch<RecordDeletion>(`/api/records/${id}`, {
    method: "DELETE",
  });
  return deletion.deletedPhotoPaths;
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
