"use client";

import { Button } from "@/components/ui/button";
import type { ShopKind } from "@/types/master";

type NewShopFormProps = {
  shopName: string;
  shopKinds: ShopKind[];
  shopKindId: number | null;
  onShopKindChange: (shopKindId: number | null) => void;
  onUse: () => void;
};

export function NewShopForm({
  shopName,
  shopKinds,
  shopKindId,
  onShopKindChange,
  onUse,
}: NewShopFormProps) {
  return (
    <div className="flex flex-col gap-2 rounded-lg border p-3 text-sm">
      <p>「{shopName}」は見つかりませんでした。</p>
      <p className="text-muted-foreground">
        業態を選ぶと、新しい店として使えます（記録するときに登録します）。
      </p>
      <p className="text-muted-foreground">
        支店名は外して入力してください。（例：セブンイレブン 大阪駅前店 →
        セブンイレブン）
      </p>
      <div className="flex gap-2">
        <select
          aria-label="業態"
          value={shopKindId ?? ""}
          onChange={(event) =>
            onShopKindChange(
              event.target.value === "" ? null : Number(event.target.value),
            )
          }
          className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-base md:text-sm"
        >
          <option value="">業態を選ぶ</option>
          {shopKinds.map((shopKind) => (
            <option key={shopKind.id} value={shopKind.id}>
              {shopKind.shopKindEmoji} {shopKind.shopKindName}
            </option>
          ))}
        </select>
        <Button type="button" disabled={shopKindId === null} onClick={onUse}>
          この店を使う
        </Button>
      </div>
    </div>
  );
}
