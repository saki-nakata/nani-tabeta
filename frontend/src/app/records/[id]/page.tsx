import { notFound, redirect } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import { searchCategories, searchRegionDetails } from "@/lib/api/masters";
import { searchRecord } from "@/lib/api/records";
import { searchMe } from "@/lib/api/users";
import { getPhotoUrls } from "@/lib/photos/url";
import type { ReactNode } from "react";
import { PhotoGallery } from "@/components/photo-gallery";

// 記録を取りに行く。見つからない（404）ときだけ null を返し、それ以外の失敗はそのまま投げる（エラーの画面になる）
async function findRecord(id: number) {
  try {
    return await searchRecord(id);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

// 食べた日を「2026年10月17日」の形にする（"2026-10-17" の文字を分けて組み立てる）
function formatEatenOn(eatenOn: string) {
  const [year, month, day] = eatenOn.split("-");
  return `${year}年${Number(month)}月${Number(day)}日`;
}

// 項目名と中身を 1 行に並べる（記録した人のニックネームなども、あとから 1 行で足せるように）
function DetailRow({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="flex gap-4">
      <dt className="w-20 shrink-0 text-muted-foreground">{label}</dt>
      <dd className="min-w-0 break-words">{children}</dd>
    </div>
  );
}

export default async function RecordPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  // 未登録なら、初回登録へ案内する（ログインなしで見られるようにするときに見直す）
  const me = await searchMe();
  if (!me) {
    redirect("/signup");
  }

  // URL の [id] の部分（/records/4 なら "4"）。1 以上の整数でなければ、記録がないのと同じ扱いにする
  const { id } = await params;
  const recordId = Number(id);
  if (!Number.isInteger(recordId) || recordId < 1) {
    notFound();
  }

  const recordDetail = await findRecord(recordId);
  if (!recordDetail) {
    notFound();
  }
  const { record, item, shop } = recordDetail;

  // 写真の URL と、エリア・分類の一覧を、同時に読む
  const [photoUrls, regionDetails, categories] = await Promise.all([
    getPhotoUrls(recordDetail.photoPaths),
    searchRegionDetails(),
    searchCategories(),
  ]);
  // 店のエリアの名前（長野県 など）と、商品の分類
  const areaName = regionDetails
    .find(({ areaList }) => areaList.some((area) => area.id === shop.areaId))
    ?.areaList.find((area) => area.id === shop.areaId)?.areaName;
  const category = categories.find(
    (category) => category.id === item.categoryId,
  );

  return (
    <main className="flex flex-1 flex-col items-center px-4 py-8">
      <article className="flex w-full max-w-md flex-col gap-6">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold break-words">{item.itemName}</h1>
          {(category || item.isSeasonal) && (
            <div className="flex flex-wrap gap-2">
              {category && (
                <span className="rounded-full bg-muted px-2.5 py-0.5 text-xs">
                  {category.categoryEmoji} {category.categoryName}
                </span>
              )}
              {item.isSeasonal && (
                <span className="rounded-full bg-muted px-2.5 py-0.5 text-xs">
                  季節限定
                </span>
              )}
            </div>
          )}
          <p className="text-sm break-words text-muted-foreground">
            {shop.shopName}
            {areaName && `（${areaName}）`}
          </p>
          <time
            dateTime={record.eatenOn}
            className="text-sm text-muted-foreground"
          >
            {formatEatenOn(record.eatenOn)}
          </time>
        </div>

        {photoUrls.length > 0 && (
          <PhotoGallery
            photoUrls={photoUrls}
            altText={`${item.itemName}の写真`}
          />
        )}

        <p className="text-2xl">
          <span aria-hidden="true">
            <span className="text-amber-400">{"★".repeat(record.rating)}</span>
            <span className="text-muted-foreground/30">
              {"★".repeat(5 - record.rating)}
            </span>
          </span>
          <span className="sr-only">評価 {record.rating}（5段階）</span>
        </p>

        {(record.price !== null || record.customNote) && (
          <dl className="flex flex-col gap-2 text-sm">
            {record.price !== null && (
              <DetailRow label="価格">
                {record.price.toLocaleString("ja-JP")} 円
              </DetailRow>
            )}
            {record.customNote && (
              <DetailRow label="カスタム">{record.customNote}</DetailRow>
            )}
          </dl>
        )}

        {record.review && (
          <section className="flex flex-col gap-2">
            <h2 className="text-sm font-medium">感想</h2>
            <p className="text-sm break-words whitespace-pre-wrap">
              {record.review}
            </p>
          </section>
        )}
      </article>
    </main>
  );
}
