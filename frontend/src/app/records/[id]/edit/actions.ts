"use server";

import { ApiError } from "@/lib/api/client";
import { deletePhotosOnServer } from "@/lib/photos/delete-on-server";
import type { RecordInput } from "@/types/record";
import { updateRecord } from "@/lib/api/records";

// 記録を保存した結果
// failed：この保存の頼みは成功しなかった（もう一度押せる）
// unknown：保存されたかわからない（返事が届かなかった・サーバーの失敗）
export type SaveRecordResult =
  | { status: "success" }
  | { status: "failed"; message: string }
  | { status: "unknown" };

// 記録を保存する（店・商品名が変わったら、バックエンドが別の商品に付けかえる）
// version は、編集画面が読んだときの版。ほかの画面で先に保存されていれば、バックエンドが 409 で断る
// 保存できたら、外された写真を Storage から消す。外された写真は、バックエンドが保存と同じ処理の中で決めて返す
export async function saveRecord(
  recordId: number,
  version: number,
  input: RecordInput,
): Promise<SaveRecordResult> {
  let removedPhotoPaths: string[];
  try {
    const result = await updateRecord(recordId, input, version);
    removedPhotoPaths = result.removedPhotoPaths;
  } catch (error) {
    // バックエンドが 400 番台を返した：入力の誤り・ログイン切れ・他人の記録・ほかの画面で先に保存された（409）などで、
    // この頼みでは保存されていない
    if (error instanceof ApiError && error.status < 500) {
      return { status: "failed", message: error.message };
    }
    // 500 番台・通信の失敗：保存されたかわからないので、写真は消さない
    console.error("記録を保存できたか確認できませんでした。", {
      recordId,
      error,
    });
    return { status: "unknown" };
  }

  // 保存できたので、外された写真を消す（消せなくても保存は成功。deletePhotosOnServer がログに残す）
  await deletePhotosOnServer(removedPhotoPaths);
  return { status: "success" };
}
