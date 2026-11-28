import { notFound } from "next/navigation";
import { UserRoundIcon } from "lucide-react";
import { searchCategories, searchRegionDetails } from "@/lib/api/masters";
import { findPublicRecord } from "@/lib/api/records";
import { searchMe } from "@/lib/api/users";
import { getPhotoUrls } from "@/lib/photos/url";
import type { ReactNode } from "react";
import { PhotoGallery } from "@/components/photo-gallery";
import { DeleteRecordButton } from "@/components/delete-record-button";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { TwemojiImage } from "@/components/twemoji-image";
import { AppHeader } from "@/components/app-header";

// 食べた日を「2026年10月17日」の形にする（"2026-10-17" の文字を分けて組み立てる）
function formatEatenOn(eatenOn: string) {
  const [year, month, day] = eatenOn.split("-");
  return `${year}年${Number(month)}月${Number(day)}日`;
}

// 項目名と中身を 1 行に並べる（価格・カスタムなど）
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

// 記録の詳細は、ログインしていない人も、未登録の人も見られる（編集・削除は、登録ずみの本人だけ）
export default async function RecordPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  // URL の [id] の部分（/records/4 なら "4"）。1 以上の整数でなければ、記録がないのと同じ扱いにする
  const { id } = await params;
  const recordId = Number(id);
  if (!Number.isInteger(recordId) || recordId < 1) {
    notFound();
  }

  // 見ている自分と記録を、同時に読む
  // me：登録ずみでログインしている自分。ログインしていない・未登録なら null（ゲスト）
  // result：記録の詳細と isGuest（ログインしていない人として読んだか）。見つからなければ null
  const [me, result] = await Promise.all([
    searchMe({ allowGuest: true }),
    findPublicRecord(recordId),
  ]);
  if (!result) {
    notFound();
  }
  const { record, item, shop, photoPaths, nickname } = result.data;
  // ゲストか：ログインしていない・未登録（me が null）、または記録をゲストとして読み直した
  const isGuest = me === null || result.isGuest;
  // 自分の記録か：登録ずみでログインしていて、ログインした人として読めていて、記録を書いた人が自分
  // 「自分の記録」の札と、編集・削除のボタンは、どちらもこの条件で出す
  const isMine = me !== null && !result.isGuest && record.userId === me.id;

  // 写真の URL と、エリア・分類の一覧を、同時に読む
  const [photoUrls, regionDetails, categories] = await Promise.all([
    getPhotoUrls(photoPaths),
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
    <>
      <AppHeader isGuest={isGuest} />
      <main className="flex flex-1 flex-col items-center px-4 py-8">
        <article className="flex w-full max-w-md flex-col gap-6">
          <div className="flex flex-col gap-1">
            <h1 className="font-heading text-2xl font-bold break-words">
              {item.itemName}
            </h1>
            {/* 商品の情報のバッジ。分類は一覧の絞り込みの分類と同じオレンジ系、季節限定は若葉色の地にして、
                おたがいと、灰色の「自分の記録」の札とを見分ける（意味は文字で伝え、色は補助） */}
            {(category || item.isSeasonal) && (
              <div className="flex flex-wrap gap-2">
                {category && (
                  <span className="rounded-full bg-category-soft px-2.5 py-0.5 text-xs">
                    <TwemojiImage emoji={category.categoryEmoji} />{" "}
                    {category.categoryName}
                  </span>
                )}
                {item.isSeasonal && (
                  <span className="rounded-full bg-seasonal-soft px-2.5 py-0.5 text-xs">
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
            {/* だれの記録か（同じ位置）：自分の記録なら控えめな札、ほかの人の記録ならニックネーム */}
            {isMine ? (
              <span className="mt-1 inline-flex w-fit items-center gap-1 rounded-sm bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
                <UserRoundIcon aria-hidden="true" className="size-3" />
                自分の記録
              </span>
            ) : (
              <p className="text-sm break-words text-muted-foreground">
                記録した人：{nickname}
              </p>
            )}
          </div>

          {photoUrls.length > 0 && (
            <PhotoGallery
              photoUrls={photoUrls}
              altText={`${item.itemName}の写真`}
            />
          )}

          <p className="text-2xl">
            <span aria-hidden="true">
              <span className="text-rating">{"★".repeat(record.rating)}</span>
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
          {/* 自分の記録にだけ、編集・削除のボタンを出す */}
          {isMine && (
            <div className="flex items-start justify-end gap-2 border-t pt-6">
              <Link
                href={`/records/${record.id}/edit`}
                className={buttonVariants({ variant: "outline" })}
              >
                編集
              </Link>
              <DeleteRecordButton recordId={record.id} />
            </div>
          )}
        </article>
      </main>
    </>
  );
}
