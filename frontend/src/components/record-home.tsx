"use client";

import { useCallback, useState } from "react";
import type { LoadRecordListResult } from "@/app/actions";
import { RecordFilters } from "@/components/record-filters";
import { RecordList } from "@/components/record-list";
import { TodayRecords } from "@/components/today-records";
import { useRecordList } from "@/hooks/use-record-list";
import { ALL_FILTER, isFiltered } from "@/lib/record-filter";
import type { Category, ShopKind } from "@/types/master";
import type { RecordListFilter } from "@/types/record";

type RecordHomeProps = {
  // サーバーで読んだ、今日の分・これまで（昨日まで）の分の最初の 1 回分
  todayResult: LoadRecordListResult;
  historyResult: LoadRecordListResult;
  // 今日・昨日の日付（"2026-11-03" の形。page.tsx で 1 回だけ決めたもの）
  today: string;
  yesterday: string;
  // 表示する今日の日付（例："11月3日(火)"）
  todayLabel: string;
  categories: Category[];
  shopKinds: ShopKind[];
  years: number[];
};

// 記録の一覧の画面（/）の中身。絞り込みを持ち、「今日」の欄と「これまで」の欄に同じ絞り込みをかける
export function RecordHome({
  todayResult,
  historyResult,
  today,
  yesterday,
  todayLabel,
  categories,
  shopKinds,
  years,
}: RecordHomeProps) {
  const [filter, setFilter] = useState(ALL_FILTER);
  // 今日の欄（今日だけ）と、これまでの欄（昨日まで）。読みこみの状態は、それぞれ別に持つ
  const todayList = useRecordList(todayResult, today, today);
  const historyList = useRecordList(historyResult, null, yesterday);
  const loadMoreHistoryPages = historyList.loadMore;

  // 絞り込みを変えたら、2 つの欄を同時に、最初から読み直す（前の条件の返事は、それぞれの欄で捨てる）
  function changeFilter(newFilter: RecordListFilter) {
    setFilter(newFilter);
    todayList.reload(newFilter);
    historyList.reload(newFilter);
  }

  // これまでの欄の無限スクロール用。絞り込みかしおりが変わったときだけ作り直す
  // （RecordList は、これが変わるたびに見張りを作り直すため）
  const loadMoreHistory = useCallback(() => {
    loadMoreHistoryPages(filter);
  }, [loadMoreHistoryPages, filter]);

  return (
    <div className="flex flex-col gap-6">
      <RecordFilters
        filter={filter}
        categories={categories}
        shopKinds={shopKinds}
        years={years}
        onChange={changeFilter}
      />

      <TodayRecords
        dateLabel={todayLabel}
        items={todayList.items}
        hasMore={todayList.nextCursor !== null}
        isLoading={todayList.isLoading}
        errorMessage={todayList.errorMessage}
        isFiltered={isFiltered(filter)}
        categories={categories}
        onLoadMore={() => todayList.loadMore(filter)}
        onRetry={() => todayList.retry(filter)}
      />

      <section className="flex flex-col gap-3 border-t pt-6">
        <div className="flex items-baseline justify-between gap-2">
          <h2 className="font-bold">これまでの記録</h2>
          <p className="text-xs text-muted-foreground">食べた日が新しい順</p>
        </div>
        <RecordList
          items={historyList.items}
          hasMore={historyList.nextCursor !== null}
          isLoading={historyList.isLoading}
          errorMessage={historyList.errorMessage}
          isFiltered={isFiltered(filter)}
          categories={categories}
          onLoadMore={loadMoreHistory}
          onRetry={() => historyList.retry(filter)}
        />
      </section>
    </div>
  );
}
