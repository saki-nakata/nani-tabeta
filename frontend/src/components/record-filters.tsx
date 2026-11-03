"use client";

import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import { cn } from "@/lib/utils";
import type { Category, ShopKind } from "@/types/master";
import type { RecordListFilter } from "@/types/record";
import { Fragment, type ReactNode } from "react";

type RecordFiltersProps = {
  // 今選んでいる絞り込み
  filter: RecordListFilter;
  categories: Category[];
  shopKinds: ShopKind[];
  // 記録がある年（新しい順）
  years: number[];
  // 押したときに、新しい絞り込みを知らせる
  onChange: (filter: RecordListFilter) => void;
};

// 押したものが今選んでいるものなら解除（すべて）、ちがえばそれを選ぶ
function selectOrClear(selectedId: number | null, id: number): number | null {
  return selectedId === id ? null : id;
}

// 横に並べて、横にスクロールできる 1 行（スワイプでも、左右の矢印でも動かせる）
// 矢印は列の外側に置くので、その分だけ列を内側に寄せる（スマホは 32px、PC は 40px）
// スマホで右端のアイコンが半分見えるのも、続きがあると気づいてもらうため
// 矢印は、押しても動かないとき（端にいる・全部見えている）は隠す
function ScrollRow({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <Carousel
      opts={{ align: "start" }}
      className="mx-8 sm:mx-10"
      aria-label={label}
    >
      <CarouselContent className="-ml-2">{children}</CarouselContent>
      {/* 読み上げの名前を日本語にする（shadcn の中の英語「Previous slide」より、こちらが優先される） */}
      <CarouselPrevious
        aria-label="前へ"
        size="icon-xs"
        className="-left-8 disabled:invisible sm:-left-12"
      />
      <CarouselNext
        aria-label="次へ"
        size="icon-xs"
        className="-right-8 disabled:invisible sm:-right-12"
      />
    </Carousel>
  );
}

// 絞り込みの種類ごとのアイコンの色（circle：ふだんの地と線、selected：選んだときの線と輪）
// Tailwind はそのままの形で書かれたクラスだけを CSS にするので、クラスの名前は組み立てずに全部書く
const EMOJI_BUTTON_STYLES = {
  category: {
    circle: "border-category/30 bg-category-soft",
    selected: "border-category ring-2 ring-category",
  },
  shopKind: {
    circle: "border-shop-kind/30 bg-shop-kind-soft",
    selected: "border-shop-kind ring-2 ring-shop-kind",
  },
};

// 「・」の後ろで折り返すと入らない名前だけ、折り返してよい所をここで決める
// 正式な名前を変えたときは、ここも直す（名前で結びつけているため）
const NAME_PARTS: Record<string, string[]> = {
  ファストフード: ["ファスト", "フード"],
  "キッチンカー・屋台": ["キッチンカー", "・屋台"],
};

// 12px の文字で、アイコンの幅（64px）に 1 行で入る文字数
const MAX_PART_LENGTH = 5;

// 名前を、折り返してよい所で分ける（ふつうは「・」の後ろ）
// 例："弁当・惣菜店" → ["弁当・", "惣菜店"]、"レストラン" → ["レストラン"]
function splitName(name: string): string[] {
  return NAME_PARTS[name] ?? name.split(/(?<=・)/);
}

// 分類・業態の丸いアイコン（絵文字の下に名前）
function EmojiButton({
  filterType,
  emoji,
  name,
  isSelected,
  onClick,
}: {
  filterType: "category" | "shopKind";
  emoji: string;
  name: string;
  isSelected: boolean;
  onClick: () => void;
}) {
  const nameParts = splitName(name);
  const isLongName = nameParts.some((part) => part.length > MAX_PART_LENGTH);

  return (
    <CarouselItem className="basis-auto pl-2">
      <button
        type="button"
        onClick={onClick}
        aria-pressed={isSelected}
        className="flex w-16 flex-col items-center gap-1"
      >
        {/* 絵文字は読み上げない（「スパゲッティ 麺類」のように、すぐ下の名前と重ならないように） */}
        <span
          aria-hidden="true"
          className={cn(
            "grid size-10 place-items-center rounded-full border text-lg sm:size-11 sm:text-xl",
            EMOJI_BUTTON_STYLES[filterType].circle,
            isSelected && EMOJI_BUTTON_STYLES[filterType].selected,
          )}
        >
          {emoji}
        </span>
        <span
          className={cn(
            "line-clamp-2 min-h-8 w-full text-center break-keep text-muted-foreground",
            isLongName ? "text-[10px]/4" : "text-xs/4",
            isSelected && "font-bold text-foreground",
          )}
        >
          {nameParts.map((part, index) => (
            <Fragment key={index}>
              {index > 0 && <wbr />}
              {part}
            </Fragment>
          ))}
        </span>
      </button>
    </CarouselItem>
  );
}

// 年のチップ（プロトタイプと同じ形）
function FilterButton({
  label,
  isSelected,
  onClick,
}: {
  label: string;
  isSelected: boolean;
  onClick: () => void;
}) {
  return (
    <CarouselItem className="basis-auto pl-2">
      <button
        type="button"
        onClick={onClick}
        aria-pressed={isSelected}
        className={cn(
          "rounded-full border px-3 py-1 text-xs whitespace-nowrap",
          isSelected
            ? "border-primary bg-primary font-bold text-primary-foreground"
            : "bg-muted text-muted-foreground",
        )}
      >
        {label}
      </button>
    </CarouselItem>
  );
}

// 絞り込みのアイコンに収まるよう、長い業態の名前だけを短くして出す（DB の正式な名前は変えない）
// 正式な名前を変えたときは、ここも直す（名前で結びつけているため）
const SHORT_SHOP_KIND_NAMES: Record<string, string> = {
  "スーパー・食品売場": "スーパー・食品",
};

// 記録の一覧の絞り込み（分類・業態・年）。それぞれ「すべて」か 1 つを選び、組み合わせて使う
export function RecordFilters({
  filter,
  categories,
  shopKinds,
  years,
  onChange,
}: RecordFiltersProps) {
  return (
    <div className="flex flex-col gap-3">
      <ScrollRow label="分類で絞り込む">
        <EmojiButton
          filterType="category"
          emoji="🍽️"
          name="すべて"
          isSelected={filter.categoryId === null}
          onClick={() => onChange({ ...filter, categoryId: null })}
        />
        {categories.map((category) => (
          <EmojiButton
            filterType="category"
            key={category.id}
            emoji={category.categoryEmoji}
            name={category.categoryName}
            isSelected={filter.categoryId === category.id}
            onClick={() =>
              onChange({
                ...filter,
                categoryId: selectOrClear(filter.categoryId, category.id),
              })
            }
          />
        ))}
      </ScrollRow>

      <ScrollRow label="業態で絞り込む">
        <EmojiButton
          filterType="shopKind"
          emoji="🏬"
          name="すべて"
          isSelected={filter.shopKindId === null}
          onClick={() => onChange({ ...filter, shopKindId: null })}
        />
        {shopKinds.map((shopKind) => (
          <EmojiButton
            filterType="shopKind"
            key={shopKind.id}
            emoji={shopKind.shopKindEmoji}
            name={
              SHORT_SHOP_KIND_NAMES[shopKind.shopKindName] ??
              shopKind.shopKindName
            }
            isSelected={filter.shopKindId === shopKind.id}
            onClick={() =>
              onChange({
                ...filter,
                shopKindId: selectOrClear(filter.shopKindId, shopKind.id),
              })
            }
          />
        ))}
      </ScrollRow>

      <ScrollRow label="年で絞り込む">
        <FilterButton
          label="すべて"
          isSelected={filter.year === null}
          onClick={() => onChange({ ...filter, year: null })}
        />
        {years.map((year) => (
          <FilterButton
            key={year}
            label={`${year}年`}
            isSelected={filter.year === year}
            onClick={() =>
              onChange({ ...filter, year: selectOrClear(filter.year, year) })
            }
          />
        ))}
      </ScrollRow>
    </div>
  );
}
