"use client";

import { useRouter } from "next/navigation";
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

type CancelEditButtonProps = {
  // 戻る先（記録の詳細）
  href: string;
  // 編集画面を開いたときから、何か変えたか
  isChanged: boolean;
};

// 編集のキャンセル。変えた所がなければすぐ戻り、あれば「保存せずに戻りますか？」と確かめる
// 保存していないので、Storage の写真はどれも消さない
export function CancelEditButton({ href, isChanged }: CancelEditButtonProps) {
  const router = useRouter();

  if (!isChanged) {
    return (
      <Button type="button" variant="outline" onClick={() => router.push(href)}>
        キャンセル
      </Button>
    );
  }

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button type="button" variant="outline">
          キャンセル
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>保存せずに戻りますか？</AlertDialogTitle>
          <AlertDialogDescription>
            変えた内容は保存されません。
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>編集を続ける</AlertDialogCancel>
          <AlertDialogAction onClick={() => router.push(href)}>
            保存せずに戻る
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
