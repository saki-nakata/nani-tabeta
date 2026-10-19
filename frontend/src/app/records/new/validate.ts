import { suggestItems } from "./actions";
import type { NewItem } from "@/components/item-picker";
import type { SelectedShop } from "@/components/shop-picker";
import { normalizeName } from "@/lib/name-normalizer";
import type { Item } from "@/types/item";

// 送信前に確かめる入力
type RecordFormValues = {
  selectedShop: SelectedShop | null;
  selectedItem: Item | null;
  newItem: NewItem;
  rating: number;
  eatenOn: string;
  today: string;
};

// 入力がそろっているかを確かめ、足りないものの案内を返す（そろっていれば空の配列）
export function validateRecordForm(values: RecordFormValues): string[] {
  const messages: string[] = [];
  if (values.selectedShop === null) {
    messages.push("店を選んでください。");
  } else if (
    values.selectedItem === null &&
    normalizeName(values.newItem.itemName) === ""
  ) {
    messages.push("商品名を入力してください。");
  }
  if (values.rating < 1) {
    messages.push("評価を選んでください。");
  }
  if (values.eatenOn === "") {
    messages.push("食べた日を入力してください。");
  } else if (values.eatenOn > values.today) {
    messages.push("食べた日に、未来の日付は選べません。");
  }
  return messages;
}

// 記録で送る商品（商品名・分類・季節限定）
export type ItemToSend = {
  itemName: string;
  categoryId: number;
  isSeasonal: boolean;
};

type ItemToSendResult =
  | { isConfirmed: true; item: ItemToSend }
  | { isConfirmed: false; message: string };

// 送る商品を決めるのに使う入力
type ItemToSendValues = {
  selectedShop: SelectedShop;
  selectedItem: Item | null;
  newItem: NewItem;
};
// 送る商品を決める
// ① 候補から選んだ商品 → ② 登録済みの店なら、もう一度探して完全に同じ名前の商品 → ③ 新しい商品（分類が必要）
export async function confirmItemToSend({
  selectedShop,
  selectedItem,
  newItem,
}: ItemToSendValues): Promise<ItemToSendResult> {
  // ① 候補から選んだ商品があれば、その値を使う
  if (selectedItem) {
    const { itemName, categoryId, isSeasonal } = selectedItem;
    return { isConfirmed: true, item: { itemName, categoryId, isSeasonal } };
  }

  const itemName = normalizeName(newItem.itemName);

  // ② 登録済みの店なら、押した時点の商品名でもう一度探し、完全に同じ名前の商品があればその値を使う
  if (selectedShop.type === "registered") {
    let items: Item[];
    try {
      items = await suggestItems(selectedShop.shop.id, itemName);
    } catch {
      // 確かめられないまま新しい商品として進めない
      return {
        isConfirmed: false,
        message:
          "商品を確認できませんでした。時間をおいて、もう一度「記録する」を押してください。",
      };
    }
    const matchedItem = items.find((item) => item.itemName === itemName);
    if (matchedItem) {
      const { categoryId, isSeasonal } = matchedItem;
      return { isConfirmed: true, item: { itemName, categoryId, isSeasonal } };
    }
  }

  // ③ 新しい商品なら、分類が必要
  if (newItem.categoryId === null) {
    return { isConfirmed: false, message: "分類を選んでください。" };
  }
  return {
    isConfirmed: true,
    item: {
      itemName,
      categoryId: newItem.categoryId,
      isSeasonal: newItem.isSeasonal,
    },
  };
}
