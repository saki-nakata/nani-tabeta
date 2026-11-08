"use server";

import { ApiError } from "@/lib/api/client";
import { deleteRecord } from "@/lib/api/records";
import { deletePhotosOnServer } from "@/lib/photos/delete-on-server";

// 記録を削除した結果
// failed：この削除の頼みは成功しなかった（もう一度押せる。前の試しですでに消えていた場合の 404 も含む）
// unknown：削除されたかわからない（返事が届かなかった・サーバーの失敗）
export type RemoveRecordResult =
  | { status: "success" }
  | { status: "failed"; message: string }
  | { status: "unknown" };

// 記録を削除し、DB から消えたことが確定してから、その写真を Storage から消す
export async function removeRecord(
  recordId: number,
): Promise<RemoveRecordResult> {
  let deletedPhotoPaths: string[];
  try {
    deletedPhotoPaths = await deleteRecord(recordId);
  } catch (error) {
    // バックエンドが 400 番台を返した：ログイン切れ・他人の記録・見つからないなどで、この頼みでは削除されていない
    if (error instanceof ApiError && error.status < 500) {
      return { status: "failed", message: error.message };
    }
    // 500 番台・通信の失敗：削除されたかわからないので、写真は消さない
    console.error("記録を削除できたか確認できませんでした。", {
      recordId,
      error,
    });
    return { status: "unknown" };
  }

  // DB の削除が確定したので、写真を消す（消せなくても記録の削除は成功。ログに残すだけ）
  await deletePhotosOnServer(deletedPhotoPaths);
  return { status: "success" };
}
