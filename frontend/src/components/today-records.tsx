import Link from "next/link";
import { RecordGrid } from "@/components/record-grid";
import { Button } from "@/components/ui/button";
import type { Category } from "@/types/master";
import type { RecordListItem } from "@/types/record";

type TodayRecordsProps = {
  // 表示する今日の日付（例："11月3日(火)"）
  dateLabel: string;
  // 今日食べた記録（読んだ分）
  items: RecordListItem[];
  // 続きがあるか（今日の記録が 20 件をこえたとき）
  hasMore: boolean;
  isLoading: boolean;
  // 読めなかったときの文（読めていれば null）
  errorMessage: string | null;
  // 何か絞り込んでいるか（0 件のときの文を分けるため）
  isFiltered: boolean;
  // 分類の一覧（写真がない記録に、分類の絵文字を出すため）
  categories: Category[];
  // 「今日の記録をもっと見る」を押したとき
  onLoadMore: () => void;
  // 「もう一度読む」を押したとき
  onRetry: () => void;
};

// 一覧のいちばん上の「今日」の欄。0 件なら、絞り込みなしは記録をうながし、絞り込みありは条件に合わないと出す
// 続きは、無限スクロールではなく「今日の記録をもっと見る」のボタンで読む（下の「これまで」の欄と、読みこみが重ならないように）
export function TodayRecords({
  dateLabel,
  items,
  hasMore,
  isLoading,
  errorMessage,
  isFiltered,
  categories,
  onLoadMore,
  onRetry,
}: TodayRecordsProps) {
  // 読み終わって 1 件もない（読み込み中・失敗は、0 件とは別に出す）
  const isEmpty =
    items.length === 0 && !hasMore && !isLoading && errorMessage === null;

  return (
    <section className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        {/* 読み上げで、見出しから見出しへ飛んだときに止まれるよう、見出しにする（見た目はバッジ） */}
        <h2 className="rounded-full bg-secondary px-2.5 py-0.5 text-xs font-bold text-secondary-foreground">
          今日
        </h2>
        <span className="text-xs text-muted-foreground">{dateLabel}</span>
      </div>

      {items.length > 0 && <RecordGrid items={items} categories={categories} />}

      {isEmpty &&
        (isFiltered ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            この条件の今日の記録はありません
          </p>
        ) : (
          <div className="flex flex-col items-center gap-1 rounded-lg border-2 border-dashed bg-muted px-4 py-6 text-center">
            <p className="font-heading text-lg font-bold">今日、なに食べた？</p>
            <p className="mb-3 text-xs text-muted-foreground">
              まだ記録がありません
            </p>
            <Button asChild className="rounded-full">
              <Link href="/records/new">＋ 記録する</Link>
            </Button>
          </div>
        ))}

      {hasMore && !isLoading && errorMessage === null && (
        <Button
          variant="outline"
          size="sm"
          onClick={onLoadMore}
          className="self-center"
        >
          今日の記録をもっと見る
        </Button>
      )}

      {/* 読み込み中を読み上げで知らせる場所。中身が変わる前からある必要があるので、いつも置いて文字だけ出し入れする */}
      {/* 空のときは画面から消す（空の行の分の余白ができないように。読み上げには残す） */}
      <p
        role="status"
        className="text-center text-sm text-muted-foreground empty:sr-only"
      >
        {isLoading && "読み込み中…"}
      </p>

      {errorMessage !== null && (
        <div className="flex flex-col items-center gap-2 py-4 text-sm">
          {/* 失敗は出たらすぐ読み上げる（ボタンまで読まないよう、文だけに付ける） */}
          <p role="alert" className="text-destructive">
            {errorMessage}
          </p>
          <Button variant="outline" onClick={onRetry}>
            もう一度読む
          </Button>
        </div>
      )}
    </section>
  );
}
