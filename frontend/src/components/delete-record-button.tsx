"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  removeRecord,
  type RemoveRecordResult,
} from "@/app/records/[id]/actions";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";

// \n で改行する（表示する <p> に whitespace-pre-line を付けている）
const UNKNOWN_MESSAGE =
  "記録を削除できたか確認できませんでした。\n記録の一覧で確かめてください。";

// 記録の削除ボタン。確認の小窓で「削除する」を押したときだけ消す
export function DeleteRecordButton({ recordId }: { recordId: number }) {
  const router = useRouter();
  // 削除の処理中か（処理中はボタンを押せなくして、二重に送らない）
  const [isDeleting, setIsDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleDelete() {
    setIsDeleting(true);
    setErrorMessage(null);
    let result: RemoveRecordResult;
    try {
      result = await removeRecord(recordId);
    } catch {
      // Server Action の呼び出しそのものが失敗した（電波が切れた など）：削除されたかわからない
      result = { status: "unknown" };
    }
    if (result.status === "success") {
      // 一覧へ移るまで、押せないままにする
      router.push("/");
      return;
    }
    setErrorMessage(
      result.status === "failed" ? result.message : UNKNOWN_MESSAGE,
    );
    setIsDeleting(false);
  }

  return (
    <div className="flex flex-col items-end gap-2 border-t pt-6">
      <AlertDialog>
        <AlertDialogTrigger asChild>
          <Button type="button" variant="destructive" disabled={isDeleting}>
            {isDeleting ? "削除しています…" : "削除"}
          </Button>
        </AlertDialogTrigger>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>この記録を削除しますか？</AlertDialogTitle>
            <AlertDialogDescription>
              写真も一緒に消え、元に戻せません。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>キャンセル</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={handleDelete}>
              削除する
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
      {errorMessage && (
        <p
          role="alert"
          className="self-stretch text-sm whitespace-pre-line text-destructive"
        >
          {errorMessage}
        </p>
      )}
    </div>
  );
}
