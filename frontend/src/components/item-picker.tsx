"use client";

import { useEffect, useState } from "react";
import { suggestItems } from "@/app/records/new/actions";
import { NewItemForm } from "@/components/new-item-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { Item } from "@/types/item";
import type { Category } from "@/types/master";
import { normalizeName } from "@/lib/name-normalizer";

// 入力中の商品（候補にないときは、記録するときに新しい商品として登録する）
export type NewItem = {
  itemName: string;
  categoryId: number | null;
  isSeasonal: boolean;
};

// 何も入力していない状態（最初の値と、店やエリアを変えたときに使う）
export const EMPTY_NEW_ITEM: NewItem = {
  itemName: "",
  categoryId: null,
  isSeasonal: false,
};

// 候補を探した結果（どの商品名で探した結果か、失敗したか）
type ItemSearchResult = {
  keyword: string;
  items: Item[];
  isError: boolean;
};

type ItemPickerProps = {
  // 選んだ店の ID（新しい店は、まだ登録していないので ID がなく null）
  shopId: number | null;
  categories: Category[];
  // 候補から選んだ登録済みの商品（選んでいなければ null）
  selectedItem: Item | null;
  onSelect: (item: Item | null) => void;
  newItem: NewItem;
  onNewItemChange: (newItem: NewItem) => void;
};

export function ItemPicker({
  shopId,
  categories,
  selectedItem,
  onSelect,
  newItem,
  onNewItemChange,
}: ItemPickerProps) {
  const [result, setResult] = useState<ItemSearchResult | null>(null);
  // 商品名の欄に入力中の文字（親が持っている）
  const keyword = newItem.itemName;

  // 商品名の入力が止まって 0.3 秒たったら、候補を探す
  useEffect(() => {
    // 新しい店には、まだ商品がないので探さない
    if (shopId === null) {
      return;
    }
    let isCancelled = false;
    const timer = setTimeout(async () => {
      try {
        const items = await suggestItems(shopId, keyword);
        if (!isCancelled) {
          setResult({ keyword, items, isError: false });
        }
      } catch {
        if (!isCancelled) {
          setResult({ keyword, items: [], isError: true });
        }
      }
    }, 300);
    return () => {
      isCancelled = true;
      clearTimeout(timer);
    };
  }, [shopId, keyword]);

  // 候補から選んだあとは、選んだ商品と「変更」ボタンだけを表示する
  if (selectedItem) {
    const category = categories.find(
      (category) => category.id === selectedItem.categoryId,
    );
    return (
      <div className="flex flex-col gap-2">
        <Label>商品</Label>
        <div className="flex items-center justify-between rounded-lg border px-2.5 py-1 text-sm">
          <div className="flex flex-col">
            <span>{selectedItem.itemName}</span>
            {category && (
              <span className="text-xs text-muted-foreground">
                分類：{category.categoryEmoji} {category.categoryName}
                {selectedItem.isSeasonal && "・季節限定"}
              </span>
            )}
          </div>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onSelect(null)}
          >
            変更
          </Button>
        </div>
      </div>
    );
  }

  // 今の商品名で探し終わっているか（まだなら、検索中）
  const searched = result !== null && result.keyword === keyword;
  // 新しい店なら、商品名を入れればすぐ新しい商品として入力できる
  // 登録済みの店なら、探し終わって、失敗しておらず、同じ名前の候補がないときだけ
  const itemName = normalizeName(keyword);
  const canUseNewItem =
    itemName !== "" &&
    (shopId === null ||
      (searched &&
        !result.isError &&
        !result.items.some((item) => item.itemName === itemName)));

  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor="itemKeyword">商品名</Label>
      <Input
        id="itemKeyword"
        placeholder="商品名を入力"
        value={keyword}
        maxLength={100}
        onChange={(event) =>
          onNewItemChange({ ...newItem, itemName: event.target.value })
        }
      />
      {result?.isError && (
        <p className="text-sm text-destructive">
          候補を探せませんでした。時間をおいて、もう一度入力してください。
        </p>
      )}
      <ul className="flex flex-col text-sm">
        {result?.items.map((suggestion) => (
          <li key={suggestion.id}>
            <button
              type="button"
              className="w-full rounded-md px-2.5 py-1.5 text-left hover:bg-muted"
              onClick={() => onSelect(suggestion)}
            >
              {suggestion.itemName}
            </button>
          </li>
        ))}
      </ul>
      {canUseNewItem && (
        <NewItemForm
          hasSuggestions={result !== null && result.items.length > 0}
          categories={categories}
          categoryId={newItem.categoryId}
          isSeasonal={newItem.isSeasonal}
          onCategoryChange={(categoryId) =>
            onNewItemChange({ ...newItem, categoryId })
          }
          onSeasonalChange={(isSeasonal) =>
            onNewItemChange({ ...newItem, isSeasonal })
          }
        />
      )}
    </div>
  );
}
