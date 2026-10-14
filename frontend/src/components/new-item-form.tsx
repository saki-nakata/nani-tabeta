"use client";

import type { Category } from "@/types/master";

type NewItemFormProps = {
  // 候補が 1 件以上出ているか（出ているときだけ「上の候補にない」と案内する）
  hasSuggestions: boolean;
  categories: Category[];
  categoryId: number | null;
  isSeasonal: boolean;
  onCategoryChange: (categoryId: number | null) => void;
  onSeasonalChange: (isSeasonal: boolean) => void;
};

export function NewItemForm({
  hasSuggestions,
  categories,
  categoryId,
  isSeasonal,
  onCategoryChange,
  onSeasonalChange,
}: NewItemFormProps) {
  return (
    <div className="flex flex-col gap-2 rounded-lg border p-3 text-sm">
      <p>
        {hasSuggestions && "上の候補にない商品を記録する場合は、"}
        分類を選んでください。「記録する」を押すと、新しい商品として登録されます。
      </p>
      <p className="text-muted-foreground">
        同じ名前の商品が登録済みの場合は、その商品の分類・季節限定を使います。
      </p>
      <select
        aria-label="分類"
        value={categoryId ?? ""}
        onChange={(event) =>
          onCategoryChange(
            event.target.value === "" ? null : Number(event.target.value),
          )
        }
        className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-base md:text-sm"
      >
        <option value="">分類を選ぶ</option>
        {categories.map((category) => (
          <option key={category.id} value={category.id}>
            {category.categoryEmoji} {category.categoryName}
          </option>
        ))}
      </select>
      <label className="flex items-center gap-2">
        <input
          type="checkbox"
          checked={isSeasonal}
          onChange={(event) => onSeasonalChange(event.target.checked)}
        />
        季節限定
      </label>
    </div>
  );
}
