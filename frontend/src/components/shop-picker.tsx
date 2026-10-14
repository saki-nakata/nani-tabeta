"use client";

import { useEffect, useState } from "react";
import { suggestShops } from "@/app/records/new/actions";
import { NewShopForm } from "@/components/new-shop-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { ShopKind } from "@/types/master";
import type { Shop } from "@/types/shop";
import { normalizeName } from "@/lib/name-normalizer";

// 選んだ店：登録済みの店か、記録するときに登録する新しい店の予定か
export type SelectedShop =
  | { type: "registered"; shop: Shop }
  | { type: "new"; shopName: string; shopKindId: number };

// 候補を探した結果（どの店名で探した結果か、失敗したか）
type ShopSearchResult = {
  keyword: string;
  shops: Shop[];
  isError: boolean;
};

type ShopPickerProps = {
  areaId: number;
  shopKinds: ShopKind[];
  selectedShop: SelectedShop | null;
  onChange: (selectedShop: SelectedShop | null) => void;
};

export function ShopPicker({
  areaId,
  shopKinds,
  selectedShop,
  onChange,
}: ShopPickerProps) {
  const [keyword, setKeyword] = useState("");
  const [result, setResult] = useState<ShopSearchResult | null>(null);
  const [newShopKindId, setNewShopKindId] = useState<number | null>(null);

  // 店名の入力が止まって 0.3 秒たったら、候補を探す
  useEffect(() => {
    let isCancelled = false;
    const timer = setTimeout(async () => {
      try {
        const shops = await suggestShops(areaId, keyword);
        if (!isCancelled) {
          setResult({ keyword, shops, isError: false });
        }
      } catch {
        if (!isCancelled) {
          setResult({ keyword, shops: [], isError: true });
        }
      }
    }, 300);
    return () => {
      isCancelled = true;
      clearTimeout(timer);
    };
  }, [areaId, keyword]);

  // 今の店名で探し終わっているか（まだなら、検索中）
  const searched = result !== null && result.keyword === keyword;
  // 今の店名で探し終わって、同じ名前の候補がなければ、新しい店として使えるようにする
  const shopName = normalizeName(keyword);
  const canUseNewShop =
    searched &&
    !result.isError &&
    shopName !== "" &&
    !result.shops.some((shop) => shop.shopName === shopName);

  // 店を選んだあとは、選んだ店と「変更」ボタンだけを表示する
  if (selectedShop) {
    const shopKind =
      selectedShop.type === "new"
        ? shopKinds.find((kind) => kind.id === selectedShop.shopKindId)
        : undefined;
    return (
      <div className="flex flex-col gap-2">
        <Label>店</Label>
        <div className="flex items-center justify-between rounded-lg border px-2.5 py-1 text-sm">
          <div className="flex flex-col">
            <span>
              {selectedShop.type === "registered"
                ? selectedShop.shop.shopName
                : selectedShop.shopName}
              {selectedShop.type === "new" && (
                <span className="ml-2 text-xs text-muted-foreground">
                  （新しい店）
                </span>
              )}
            </span>
            {shopKind && (
              <span className="text-xs text-muted-foreground">
                業態：{shopKind.shopKindEmoji} {shopKind.shopKindName}
              </span>
            )}
          </div>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onChange(null)}
          >
            変更
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor="shopKeyword">店名</Label>
      <Input
        id="shopKeyword"
        placeholder="店名の一部を入力"
        value={keyword}
        maxLength={100}
        onChange={(event) => setKeyword(event.target.value)}
      />
      {result?.isError && (
        <p className="text-sm text-destructive">
          候補を探せませんでした。時間をおいて、もう一度入力してください。
        </p>
      )}
      <ul className="flex flex-col text-sm">
        {result?.shops.map((suggestion) => (
          <li key={suggestion.id}>
            <button
              type="button"
              className="w-full rounded-md px-2.5 py-1.5 text-left hover:bg-muted"
              onClick={() => onChange({ type: "registered", shop: suggestion })}
            >
              {suggestion.shopName}
            </button>
          </li>
        ))}
      </ul>
      {canUseNewShop && (
        <NewShopForm
          shopName={shopName}
          shopKinds={shopKinds}
          shopKindId={newShopKindId}
          onShopKindChange={setNewShopKindId}
          onUse={() => {
            if (newShopKindId !== null) {
              onChange({ type: "new", shopName, shopKindId: newShopKindId });
            }
          }}
        />
      )}
    </div>
  );
}
