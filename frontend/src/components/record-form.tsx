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
import {
  confirmItemToSend,
  validateRecordForm,
} from "@/app/records/new/validate";
import { Button } from "@/components/ui/button";
import { useRouter } from "next/navigation";
import {
  registerNewShop,
  submitRecord,
  type RegisterNewShopResult,
  type SubmitRecordResult,
} from "@/app/records/new/actions";
import { deletePhotos, uploadPhotos } from "@/lib/photos/upload";

// 店を登録できなかったときの案内
const SHOP_FAILED_MESSAGE =
  "店を登録できませんでした。時間をおいて、もう一度「記録する」を押してください。";
// 写真を置けなかったときの案内
const PHOTO_FAILED_MESSAGE =
  "写真をアップロードできませんでした。写真の容量が大きすぎるか、通信が不安定な可能性があります。別の写真を選ぶか、時間をおいてもう一度「記録する」を押してください。";
// 記録できたか確かめられなかったときの案内（もう一度押すと重複しうることも伝える）
const UNKNOWN_RECORD_MESSAGE =
  "記録できたか確認できませんでした。すでに登録されている可能性があります。もう一度「記録する」を押すと、同じ記録が重複することがあります。";

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
  const router = useRouter();

  const [photos, setPhotos] = useState<SelectedPhoto[]>([]);
  const [areaId, setAreaId] = useState(initialAreaId);
  const [selectedShop, setSelectedShop] = useState<SelectedShop | null>(null);
  const [selectedItem, setSelectedItem] = useState<Item | null>(null);
  const [newItem, setNewItem] = useState<NewItem>(EMPTY_NEW_ITEM);
  const [eatenOn, setEatenOn] = useState(today);
  const [rating, setRating] = useState(0);
  const [review, setReview] = useState("");
  const [price, setPrice] = useState("");
  // 「記録する」を押したときの案内（足りないもの・失敗の理由）
  const [errorMessages, setErrorMessages] = useState<string[]>([]);
  // 確かめている・送っている途中か（その間は入力もボタンも押せなくする）
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [customNote, setCustomNote] = useState("");
  const [editPrice, setEditPrice] = useState(false);

  // 店やエリアを変えたら、選んだ商品も入力中の商品も消す（別の店の商品が残らないように）
  function clearItem() {
    setSelectedItem(null);
    setNewItem(EMPTY_NEW_ITEM);
  }

  // 「記録する」を押したとき
  async function handleSubmit() {
    setIsSubmitting(true);
    setErrorMessages([]);
    const messages = await sendRecord();
    // 止まったときだけ、案内を出して押せる状態に戻す
    // 成功したときは、詳細画面へ移るまで押せないままにする（二重に登録しないように）
    if (messages.length > 0) {
      setErrorMessages(messages);
      setIsSubmitting(false);
    }
  }

  // 入力を確かめて、店の登録 → 写真のアップロード → 記録の登録 を行う
  // 成功したら詳細画面へ移って空の配列を返す。途中で止まったら、案内を返す
  async function sendRecord(): Promise<string[]> {
    // ① 入力がそろっているか
    const messages = validateRecordForm({
      selectedShop,
      selectedItem,
      newItem,
      rating,
      eatenOn,
      today,
    });
    if (messages.length > 0) {
      return messages;
    }
    // ここで確かめると、この先は店もエリアもあると TypeScript に伝わる（店があれば、エリアも必ずある）
    if (selectedShop === null || areaId === null) {
      return ["店を選んでください。"];
    }

    // ② 送る商品を決める（候補を選んでいなければ、もう一度探して確かめる）
    const itemToSendResult = await confirmItemToSend({
      selectedShop,
      selectedItem,
      newItem,
    });
    if (!itemToSendResult.isConfirmed) {
      return [itemToSendResult.message];
    }

    // ③ 新しい店なら、店を登録して ID をもらう（同じ店名・エリアの店があれば、その店の ID が返る）
    let shopId: number;
    if (selectedShop.type === "registered") {
      shopId = selectedShop.shop.id;
    } else {
      let shopResult: RegisterNewShopResult;
      try {
        shopResult = await registerNewShop({
          shopName: selectedShop.shopName,
          areaId,
          shopKindId: selectedShop.shopKindId,
        });
      } catch {
        // 返事が届かなかった（まだ写真は置いていないので、後片付けはない）
        return [SHOP_FAILED_MESSAGE];
      }
      if (!shopResult.isSuccess) {
        return [SHOP_FAILED_MESSAGE];
      }
      shopId = shopResult.shop.id;
    }

    // ④ 写真を縮めて置く（途中で失敗したら、uploadPhotos が置いた分の削除を試みてから投げる）
    let photoPaths: string[];
    try {
      photoPaths = await uploadPhotos(
        photos.map((photo) => photo.file),
        "records",
      );
    } catch {
      return [PHOTO_FAILED_MESSAGE];
    }

    // ⑤ 記録を登録する
    const normalizedPrice = normalizePrice(price);
    let recordResult: SubmitRecordResult;
    try {
      recordResult = await submitRecord({
        shopId,
        ...itemToSendResult.item,
        eatenOn,
        rating,
        review: review.trim() === "" ? null : review,
        customNote: customNote.trim() === "" ? null : customNote,
        price: normalizedPrice === "" ? null : Number(normalizedPrice),
        photoPaths,
      });
    } catch {
      // 返事が届かなかった：登録されたかわからないので、写真は消さない
      return [UNKNOWN_RECORD_MESSAGE];
    }
    if (recordResult.status === "failed") {
      // 確実に登録されていないので、置いた写真の削除を試みる（消せなかった分は、日次バッチで片付ける予定。まだ作っていない）
      await deletePhotos(photoPaths);
      return [recordResult.message];
    }
    if (recordResult.status === "unknown") {
      return [UNKNOWN_RECORD_MESSAGE];
    }

    // ⑥ 詳細画面へ移る（移れなくても登録は成功しているので、写真は消さず、押せないままにする）
    router.push(`/records/${recordResult.recordId}`);
    return [];
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
    <fieldset
      disabled={isSubmitting}
      className="flex w-full max-w-md min-w-0 flex-col gap-6"
    >
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
          onSelectedItemChange={setSelectedItem}
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
      {errorMessages.length > 0 && (
        <ul
          role="alert"
          className="flex flex-col gap-1 text-sm text-destructive"
        >
          {errorMessages.map((message) => (
            <li key={message}>{message}</li>
          ))}
        </ul>
      )}
      <Button type="button" onClick={handleSubmit}>
        {isSubmitting ? "記録しています…" : "記録する"}
      </Button>
    </fieldset>
  );
}
