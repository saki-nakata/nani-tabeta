import { notFound, redirect } from "next/navigation";
import { RecordForm } from "@/components/record-form";
import {
  searchCategories,
  searchRegionDetails,
  searchShopKinds,
} from "@/lib/api/masters";
import { findRecord } from "@/lib/api/records";
import { searchMe } from "@/lib/api/users";
import type { SelectedPhoto } from "@/components/photo-picker";
import { getPhotoUrls } from "@/lib/photos/url";

export default async function EditRecordPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  // 未登録なら、初回登録へ案内する
  const me = await searchMe();
  if (!me) {
    redirect("/signup");
  }

  // URL の [id] の部分。1 以上の整数でなければ、記録がないのと同じ扱いにする
  const { id } = await params;
  const recordId = Number(id);
  if (!Number.isInteger(recordId) || recordId < 1) {
    notFound();
  }

  // 記録がない、または自分の記録でなければ、見つからない扱いにする
  // （詳細画面は他の人も見られるが、編集画面は本人だけ。保存の API も本人以外は 403 になる）
  const recordDetail = await findRecord(recordId);
  if (!recordDetail || recordDetail.record.userId !== me.id) {
    notFound();
  }

  // 今日の日付（日本時間）。未来の日付を選べないようにするために使う
  const today = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Tokyo",
  }).format(new Date());
  const [regionDetails, shopKinds, categories, photoUrls] = await Promise.all([
    searchRegionDetails(),
    searchShopKinds(),
    searchCategories(),
    getPhotoUrls(recordDetail.photoPaths),
  ]);

  // 今の写真を、写真の欄に並べる形にする（保存済みの写真は、パスを id にする）
  const initialPhotos: SelectedPhoto[] = recordDetail.photoPaths.map(
    (path, index) => ({ type: "saved", id: path, path, url: photoUrls[index] }),
  );

  return (
    <main className="flex flex-1 flex-col items-center gap-6 px-4 py-8">
      <h1 className="text-2xl font-bold">記録を編集する</h1>
      <RecordForm
        today={today}
        regionDetails={regionDetails}
        shopKinds={shopKinds}
        categories={categories}
        initialAreaId={recordDetail.shop.areaId}
        editingRecord={recordDetail}
        initialPhotos={initialPhotos}
      />
    </main>
  );
}
