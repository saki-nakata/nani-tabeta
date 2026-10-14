import { redirect } from "next/navigation";
import { RecordForm } from "@/components/record-form";
import { searchMe } from "@/lib/api/users";
import { cookies } from "next/headers";
import { LAST_AREA_COOKIE } from "@/lib/last-area";
import {
  searchCategories,
  searchRegionDetails,
  searchShopKinds,
} from "@/lib/api/masters";

export default async function NewRecordPage() {
  // 未登録なら、写真を選んだりアップロードしたりする前に、初回登録へ案内する
  const me = await searchMe();
  if (!me) {
    redirect("/signup");
  }
  // 今日の日付（日本時間、"2026-10-08" の形）。食べた日の最初の値と、未来の日付を選べないようにするために使う
  const today = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Tokyo",
  }).format(new Date());
  // 入力欄で使う一覧を、3 つ同時に読む（おたがいに関係がないので、順番に待たなくてよい）
  // regionDetails：エリアの一覧（地方ごと）。店を探すときに、まずエリアを選んでもらう
  // shopKinds：業態の一覧。候補にない店を、新しい店として使うときに選んでもらう
  // categories：分類の一覧。候補にない商品を、新しい商品として使うときに選んでもらう
  const [regionDetails, shopKinds, categories] = await Promise.all([
    searchRegionDetails(),
    searchShopKinds(),
    searchCategories(),
  ]);
  // 前回選んだエリア（Cookie に覚えてある）。今もあるエリアなら、最初の値にする
  const cookieStore = await cookies();
  const lastAreaId = Number(cookieStore.get(LAST_AREA_COOKIE)?.value);
  const initialAreaId = regionDetails.some(({ areaList }) =>
    areaList.some((area) => area.id === lastAreaId),
  )
    ? lastAreaId
    : null;

  return (
    <main className="flex flex-1 flex-col items-center gap-6 px-4 py-8">
      <h1 className="text-2xl font-bold">記録する</h1>
      <RecordForm
        today={today}
        regionDetails={regionDetails}
        shopKinds={shopKinds}
        categories={categories}
        initialAreaId={initialAreaId}
      />
    </main>
  );
}
