"use client";

import Link from "next/link";
import { useState } from "react";
import { cn } from "@/lib/utils";
import type { RecordListItem } from "@/types/record";

type RecordCardProps = {
  item: RecordListItem;
  // 写真がない・読めないときに出す、分類の絵文字
  categoryEmoji: string;
};

// 記録の 1 マス。写真の上に店名・商品名・評価・食べた日を重ね、押すと記録の詳細へ移る
// 記録の一覧のほか、タイムライン・いいねした記録・検索の結果でも使う
export function RecordCard({ item, categoryEmoji }: RecordCardProps) {
  // 写真を読めなかった（消えた・通信の失敗など）ときは、写真がないときと同じく絵文字にする
  const [isPhotoError, setIsPhotoError] = useState(false);
  const photoUrl = isPhotoError ? null : item.mainPhotoUrl;
  // 食べた日 "2026-10-26" を "10/26" にする（文字を分けて組み立てる。日付の計算はしない）
  const [, month, day] = item.eatenOn.split("-");

  return (
    <Link
      href={`/records/${item.id}`}
      className={cn(
        "relative block aspect-3/4 overflow-hidden rounded-lg",
        photoUrl ? "bg-muted" : "bg-secondary",
      )}
    >
      {photoUrl ? (
        // eslint-disable-next-line @next/next/no-img-element -- 写真は置くときに WebP・長辺 1200px に縮めてあるので、Next.js の Image は使わない
        <img
          src={photoUrl}
          alt={`${item.itemName}の写真`}
          onError={() => setIsPhotoError(true)}
          className="absolute inset-0 size-full object-cover"
        />
      ) : (
        <span
          aria-hidden="true"
          className="absolute inset-0 grid place-items-center text-4xl"
        >
          {categoryEmoji}
        </span>
      )}

      {item.photoCount >= 2 && (
        <span className="absolute top-1.5 right-1.5 rounded-full bg-black/70 px-2 py-0.5 text-xs font-bold text-white">
          <span aria-hidden="true">⧉ </span>
          {item.photoCount}
          <span className="sr-only">枚の写真</span>
        </span>
      )}

      {/* 写真の上は、下の方を暗くして白い文字にする。写真がないマスは、薄い藍の地にふつうの文字 */}
      <div
        className={cn(
          "absolute inset-x-0 bottom-0 px-2 pt-6 pb-2",
          photoUrl
            ? "bg-linear-to-t from-black/85 via-black/55 to-transparent text-white"
            : "text-foreground",
        )}
      >
        <p
          className={cn(
            "truncate text-xs",
            photoUrl ? "text-white/80" : "text-muted-foreground",
          )}
        >
          {item.shopName}
        </p>
        <p className="line-clamp-2 text-sm font-bold">{item.itemName}</p>
        <div
          className={cn(
            "mt-0.5 flex items-center justify-between text-xs",
            photoUrl ? "text-white/80" : "text-muted-foreground",
          )}
        >
          <span>
            <span aria-hidden="true">
              <span className="text-rating">{"★".repeat(item.rating)}</span>
              <span
                className={photoUrl ? "text-white/30" : "text-foreground/20"}
              >
                {"★".repeat(5 - item.rating)}
              </span>
            </span>
            <span className="sr-only">評価 {item.rating}（5段階）</span>
          </span>
          <time dateTime={item.eatenOn}>
            {Number(month)}/{Number(day)}
          </time>
        </div>
      </div>
    </Link>
  );
}
