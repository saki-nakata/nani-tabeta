import { PencilIcon } from "lucide-react";
import Link from "next/link";
import { DeleteRecordButton } from "@/components/delete-record-button";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type RecordCardActionsProps = {
  recordId: number;
  // 削除したとき（その記録のカードを、一覧から消す）
  onDeleted: () => void;
};

// 記録の一覧のカードに重ねる「編集」「削除」
// パソコン（マウスを乗せられる画面）では、マウスを乗せたとき・キーボードで移ったとき・削除に失敗した文があるときに見える
// タッチの画面では、キーボードで移ったとき（外付けキーボードなど）だけ出す。指で触っただけでは出ない
export function RecordCardActions({
  recordId,
  onDeleted,
}: RecordCardActionsProps) {
  return (
    <div className="absolute top-1.5 right-1.5 hidden max-w-[calc(100%-0.75rem)] items-start gap-1.5 opacity-0 transition-opacity group-focus-within:flex group-focus-within:opacity-100 group-hover:opacity-100 has-[[role=alert]]:flex has-[[role=alert]]:opacity-100 [@media(hover:hover)]:flex">
      {/* 「編集」と「削除」は、それぞれ白い台にのせて別々のボタンにする（写真の上でも見えるように） */}
      <span className="rounded-md bg-background shadow-sm">
        <Link
          href={`/records/${recordId}/edit`}
          aria-label="編集"
          title="編集"
          className={cn(
            buttonVariants({ variant: "ghost", size: "icon-xs" }),
            "bg-primary/10 text-primary hover:bg-primary hover:text-primary-foreground",
          )}
        >
          <PencilIcon />
        </Link>
      </span>
      <span className="rounded-md bg-background shadow-sm">
        <DeleteRecordButton
          recordId={recordId}
          onDeleted={onDeleted}
          size="icon-xs"
        />
      </span>
    </div>
  );
}
