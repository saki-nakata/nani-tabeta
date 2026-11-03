"use client";

import { useEffect, useRef } from "react";
import { RecordGrid } from "@/components/record-grid";
import { Button } from "@/components/ui/button";
import { groupByYear } from "@/lib/year-groups";
import type { Category } from "@/types/master";
import type { RecordListItem } from "@/types/record";

type RecordListProps = {
  // これまで（昨日まで）の記録（読んだ分）
  items: RecordListItem[];
  // 続きがあるか
  hasMore: boolean;
  isLoading: boolean;
  // 読めなかったときの文（読めていれば null）
  errorMessage: string | null;
  // 何か絞り込んでいるか（0 件のときの文を分けるため）
  isFiltered: boolean;
  // 分類の一覧（写真がない記録に、分類の絵文字を出すため）
  categories: Category[];
  // 下の端の目印が画面に近づいたとき（続きを読む）
  onLoadMore: () => void;
  // 「もう一度読む」を押したとき
  onRetry: () => void;
};

// 「これまでの記録」の欄。年が変わる所に見出しを入れ、下まで来たら続きを読む
export function RecordList({
  items,
  hasMore,
  isLoading,
  errorMessage,
  isFiltered,
  categories,
  onLoadMore,
  onRetry,
}: RecordListProps) {
  // 一覧の下の端に置く、見えない目印。これが画面に入ったら続きを読む
  const scrollMarkerRef = useRef<HTMLDivElement>(null);

  // 目印が画面に近づいたら、続きを読む（続きがない・失敗しているときは見張らない）
  useEffect(() => {
    const scrollMarker = scrollMarkerRef.current;
    if (scrollMarker === null || !hasMore || errorMessage !== null) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          onLoadMore();
        }
      },
      { rootMargin: "0px 0px 300px 0px" },
    );
    observer.observe(scrollMarker);
    return () => observer.disconnect();
  }, [onLoadMore, hasMore, errorMessage]);

  const yearGroups = groupByYear(items);
  // 読み終わって 1 件もない（読み込み中・失敗は、0 件とは別に出す）
  const isEmpty =
    yearGroups.length === 0 && !hasMore && !isLoading && errorMessage === null;

  return (
    <div className="flex flex-col gap-6">
      {isEmpty && (
        <p className="py-12 text-center text-sm text-muted-foreground">
          {isFiltered
            ? "この条件の記録はありません"
            : "これまでの記録はまだありません"}
        </p>
      )}

      {yearGroups.map((group) => (
        <section key={group.year} className="flex flex-col gap-2">
          <h3 className="text-sm font-bold text-muted-foreground">
            {group.year}年
          </h3>
          <RecordGrid items={group.items} categories={categories} />
        </section>
      ))}

      <div ref={scrollMarkerRef} />

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
    </div>
  );
}
