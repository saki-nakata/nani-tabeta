"use client";

import { useEffect, useRef, useState } from "react";
import { PhotoPicker, type SelectedPhoto } from "@/components/photo-picker";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { StarRating } from "@/components/star-rating";
import { AreaPicker } from "@/components/area-picker";
import { saveLastAreaId } from "@/lib/last-area";
import { ShopPicker, type SelectedShop } from "@/components/shop-picker";
import type { Category, RegionDetail, ShopKind } from "@/types/master";
import {
  EMPTY_NEW_ITEM,
  ItemPicker,
  type NewItem,
} from "@/components/item-picker";
import type { Item } from "@/types/item";
import { normalizePrice } from "@/lib/price";

type RecordFormProps = {
  today: string;
  regionDetails: RegionDetail[];
  shopKinds: ShopKind[];
  categories: Category[];
  initialAreaId: number | null;
};

export function RecordForm({
  today,
  regionDetails,
  shopKinds,
  categories,
  initialAreaId,
}: RecordFormProps) {
  const [photos, setPhotos] = useState<SelectedPhoto[]>([]);
  const [areaId, setAreaId] = useState(initialAreaId);
  const [selectedShop, setSelectedShop] = useState<SelectedShop | null>(null);
  const [selectedItem, setSelectedItem] = useState<Item | null>(null);
  const [newItem, setNewItem] = useState<NewItem>(EMPTY_NEW_ITEM);
  const [eatenOn, setEatenOn] = useState(today);
  const [rating, setRating] = useState(0);
  const [review, setReview] = useState("");
  const [price, setPrice] = useState("");
  const [customNote, setCustomNote] = useState("");
  const [editPrice, setEditPrice] = useState(false);

  // 店やエリアを変えたら、選んだ商品も入力中の商品も消す（別の店の商品が残らないように）
  function clearItem() {
    setSelectedItem(null);
    setNewItem(EMPTY_NEW_ITEM);
  }

  // 画面を離れるときに片付けられるよう、今の写真の一覧を控えておく
  const photosRef = useRef(photos);
  useEffect(() => {
    photosRef.current = photos;
  }, [photos]);

  // 画面を離れるときに、残っているプレビュー用の URL を片付ける
  useEffect(
    () => () => {
      photosRef.current.forEach((selectedPhoto) =>
        URL.revokeObjectURL(selectedPhoto.previewUrl),
      );
    },
    [],
  );

  return (
    <div className="flex w-full max-w-md flex-col gap-6">
      <PhotoPicker photos={photos} onChange={setPhotos} />
      <AreaPicker
        regionDetails={regionDetails}
        areaId={areaId}
        hint={
          areaId === null
            ? "地方・都道府県を選択後、店名検索欄が表示されます"
            : undefined
        }
        onChange={(nextAreaId) => {
          setAreaId(nextAreaId);
          setSelectedShop(null);
          clearItem();
          if (nextAreaId !== null) {
            saveLastAreaId(nextAreaId);
          }
        }}
      />

      {areaId !== null && (
        <ShopPicker
          key={areaId}
          areaId={areaId}
          shopKinds={shopKinds}
          selectedShop={selectedShop}
          onChange={(nextShop) => {
            setSelectedShop(nextShop);
            clearItem();
          }}
        />
      )}

      {selectedShop !== null && (
        <ItemPicker
          shopId={
            selectedShop.type === "registered" ? selectedShop.shop.id : null
          }
          categories={categories}
          selectedItem={selectedItem}
          onSelect={setSelectedItem}
          newItem={newItem}
          onNewItemChange={setNewItem}
        />
      )}

      <div className="flex flex-col gap-2">
        <Label htmlFor="eatenOn">食べた日</Label>
        <Input
          id="eatenOn"
          type="date"
          value={eatenOn}
          max={today}
          onChange={(event) => setEatenOn(event.target.value)}
        />
      </div>

      <div className="flex flex-col gap-2">
        <Label>評価</Label>
        <StarRating value={rating} onChange={setRating} />
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="review">感想</Label>
        <Textarea
          id="review"
          value={review}
          maxLength={1000}
          onChange={(event) => setReview(event.target.value)}
        />
        <p className="text-right text-xs text-muted-foreground">
          {review.length} / 1000
        </p>
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="price">価格</Label>
        <div className="flex items-center gap-2">
          <Input
            id="price"
            type="text"
            inputMode="numeric"
            value={
              editPrice || price === ""
                ? price
                : Number(price).toLocaleString("ja-JP")
            }
            onFocus={() => setEditPrice(true)}
            onChange={(event) => setPrice(event.target.value)}
            onBlur={() => {
              setPrice(normalizePrice(price));
              setEditPrice(false);
            }}
          />
          <span className="text-sm">円</span>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="customNote">カスタム</Label>
        <Textarea
          id="customNote"
          placeholder="例：麺かため、ネギ多め"
          value={customNote}
          maxLength={500}
          onChange={(event) => setCustomNote(event.target.value)}
        />
        <p className="text-right text-xs text-muted-foreground">
          {customNote.length} / 500
        </p>
      </div>
    </div>
  );
}
