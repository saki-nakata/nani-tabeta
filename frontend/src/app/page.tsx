import { redirect } from "next/navigation";
import { AppHeader } from "@/components/app-header";
import { RecordHome } from "@/components/record-home";
import { searchCategories, searchShopKinds } from "@/lib/api/masters";
import { searchRecordYears } from "@/lib/api/records";
import { searchMe } from "@/lib/api/users";
import { dayBefore } from "@/lib/dates";
import { ALL_FILTER } from "@/lib/record-filter";
import type { Category, ShopKind } from "@/types/master";
import { loadRecordList, type LoadRecordListResult } from "./actions";

export default async function Home() {
  const me = await searchMe();
  if (!me) {
    redirect("/signup");
  }

  // 今日の日付（日本時間）。今日・昨日・表示の日付を、全部この 1 回の now から作る
  // "2026-11-03" は範囲に使い、"11月3日(火)" は表示に使う
  const now = new Date();
  const today = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Tokyo",
  }).format(now);
  const yesterday = dayBefore(today);
  const todayLabel = new Intl.DateTimeFormat("ja-JP", {
    timeZone: "Asia/Tokyo",
    month: "long",
    day: "numeric",
    weekday: "short",
  }).format(now);

  // 今日の分（今日だけ）・これまでの分（昨日まで）と、絞り込みに使う分類・業態・年の一覧を、同時に読む
  // 記録の 2 つは、読めなくても画面全体をエラーにせず、それぞれの欄で失敗を出す
  const [todayResult, historyResult, categories, shopKinds, years]: [
    LoadRecordListResult,
    LoadRecordListResult,
    Category[],
    ShopKind[],
    number[],
  ] = await Promise.all([
    loadRecordList(ALL_FILTER, { eatenFrom: today, eatenTo: today }, null),
    loadRecordList(ALL_FILTER, { eatenFrom: null, eatenTo: yesterday }, null),
    searchCategories(),
    searchShopKinds(),
    searchRecordYears(),
  ]);

  return (
    <>
      <AppHeader />
      <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 px-4 py-6">
        <p className="text-sm">{me.nickname} さん、こんにちは</p>
        <RecordHome
          todayResult={todayResult}
          historyResult={historyResult}
          today={today}
          yesterday={yesterday}
          todayLabel={todayLabel}
          categories={categories}
          shopKinds={shopKinds}
          years={years}
        />
      </main>
    </>
  );
}
