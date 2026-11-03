"use server";

import { ApiError } from "@/lib/api/client";
import { searchRecordList } from "@/lib/api/records";
import { getPhotoUrlsOrNull } from "@/lib/photos/url";
import type {
  RecordListCursor,
  RecordListFilter,
  RecordListItem,
  RecordListRange,
} from "@/types/record";

// 記録の一覧を読んだ結果
// success：読めた（nextCursor が null なら、続きはない）
// failed：読めなかった（0 件とは別にして、画面で失敗とやり直しを出す）
export type LoadRecordListResult =
  | {
      status: "success";
      items: RecordListItem[];
      nextCursor: RecordListCursor | null;
    }
  | { status: "failed"; message: string };

// 記録の一覧を 1 回分読み、代表写真のパスを表示用の URL に変えて返す
// 最初の表示（page.tsx）と、続き・絞り込みの読み直し（一覧の部品）の両方から呼ぶ
export async function loadRecordList(
  filter: RecordListFilter,
  range: RecordListRange,
  cursor: RecordListCursor | null,
): Promise<LoadRecordListResult> {
  try {
    const page = await searchRecordList(filter, range, cursor);
    const photoUrls = await getPhotoUrlsOrNull(
      page.records.map((card) => card.mainPhotoPath),
    );
    const items = page.records.map((card, index) => ({
      ...card,
      mainPhotoUrl: photoUrls[index],
    }));
    return { status: "success", items, nextCursor: page.nextCursor };
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return {
        status: "failed",
        message: "ログインの期限が切れました。もう一度ログインしてください。",
      };
    }
    console.error("記録の一覧を読めませんでした。", error);
    return { status: "failed", message: "記録を読み込めませんでした。" };
  }
}
