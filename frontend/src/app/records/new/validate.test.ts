import { afterEach, describe, expect, test, vi } from "vitest";
import type { NewItem } from "@/components/item-picker";
import type { SelectedShop } from "@/components/shop-picker";
import type { Item } from "@/types/item";
import { confirmItemToSend, validateRecordForm } from "./validate";

const { suggestItems } = vi.hoisted(() => ({ suggestItems: vi.fn() }));
vi.mock("./actions", () => ({ suggestItems }));

const registeredShop: SelectedShop = {
  type: "registered",
  shop: {
    id: 4,
    shopName: "スタバ",
    areaId: 20,
    shopKindId: 3,
    isClosed: false,
  },
};
const newShop: SelectedShop = { type: "new", shopName: "一蘭", shopKindId: 6 };

const karaage: Item = {
  id: 7,
  shopId: 4,
  itemName: "からあげクン",
  categoryId: 4,
  isSeasonal: false,
};

function newItemOf(
  itemName: string,
  categoryId: number | null = null,
  isSeasonal = false,
): NewItem {
  return { itemName, categoryId, isSeasonal };
}

afterEach(() => {
  vi.resetAllMocks();
});

describe("validateRecordForm", () => {
  const filled = {
    selectedShop: registeredShop,
    selectedItem: null,
    newItem: newItemOf("から揚げ棒", 4),
    rating: 4,
    eatenOn: "2026-10-16",
    today: "2026-10-16",
  };

  test("すべてそろっていれば、案内は空であること", () => {
    const actual = validateRecordForm(filled);

    expect(actual).toEqual([]);
  });

  test("候補から選んだ商品があれば、商品名の欄が空でもよいこと", () => {
    const actual = validateRecordForm({
      ...filled,
      selectedItem: karaage,
      newItem: newItemOf(""),
    });

    expect(actual).toEqual([]);
  });

  test("足りないものが複数あれば、まとめて返すこと", () => {
    const actual = validateRecordForm({
      ...filled,
      selectedShop: null,
      rating: 0,
      eatenOn: "",
    });

    expect(actual).toEqual([
      "店を選んでください。",
      "評価を選んでください。",
      "食べた日を入力してください。",
    ]);
  });

  test("店を選んでいないときは、商品名の案内を出さないこと（商品の欄が表示されていないため）", () => {
    const actual = validateRecordForm({
      ...filled,
      selectedShop: null,
      newItem: newItemOf(""),
    });

    expect(actual).toEqual(["店を選んでください。"]);
  });

  test("商品名が空白だけなら、商品名の案内を出すこと", () => {
    const actual = validateRecordForm({
      ...filled,
      newItem: newItemOf(" 　 "),
    });

    expect(actual).toEqual(["商品名を入力してください。"]);
  });

  test("食べた日が今日より後なら、案内を出すこと", () => {
    const actual = validateRecordForm({ ...filled, eatenOn: "2026-10-17" });

    expect(actual).toEqual(["食べた日に、未来の日付は選べません。"]);
  });

  test("食べた日が今日より前なら、案内を出さないこと", () => {
    const actual = validateRecordForm({ ...filled, eatenOn: "2025-12-31" });

    expect(actual).toEqual([]);
  });
});

describe("confirmItemToSend", () => {
  test("① 候補から選んだ商品があれば、探し直さずにその商品の値を使うこと", async () => {
    const actual = await confirmItemToSend({
      selectedShop: registeredShop,
      selectedItem: karaage,
      newItem: newItemOf("から", 8, true),
    });

    expect(actual).toEqual({
      isConfirmed: true,
      item: { itemName: "からあげクン", categoryId: 4, isSeasonal: false },
    });
    expect(suggestItems).not.toHaveBeenCalled();
  });

  test("② 登録済みの店で、完全に同じ名前の商品があれば、その商品の分類・季節限定を使うこと", async () => {
    suggestItems.mockResolvedValue([karaage]);

    const actual = await confirmItemToSend({
      selectedShop: registeredShop,
      selectedItem: null,
      newItem: newItemOf("からあげクン", 8, true),
    });

    expect(actual).toEqual({
      isConfirmed: true,
      item: { itemName: "からあげクン", categoryId: 4, isSeasonal: false },
    });
    expect(suggestItems).toHaveBeenCalledWith(4, "からあげクン");
  });

  test("② 空白を整えると同じ名前になる場合も、その商品を使うこと", async () => {
    const latte: Item = {
      ...karaage,
      id: 8,
      itemName: "抹茶 ラテ",
      categoryId: 10,
    };
    suggestItems.mockResolvedValue([latte]);

    const actual = await confirmItemToSend({
      selectedShop: registeredShop,
      selectedItem: null,
      newItem: newItemOf("　抹茶　ラテ "),
    });

    expect(actual).toEqual({
      isConfirmed: true,
      item: { itemName: "抹茶 ラテ", categoryId: 10, isSeasonal: false },
    });
    expect(suggestItems).toHaveBeenCalledWith(4, "抹茶 ラテ");
  });

  test("② 部分一致の候補は使わず、新しい商品として入力した値を使うこと", async () => {
    suggestItems.mockResolvedValue([karaage]);

    const actual = await confirmItemToSend({
      selectedShop: registeredShop,
      selectedItem: null,
      newItem: newItemOf("から", 8, true),
    });

    expect(actual).toEqual({
      isConfirmed: true,
      item: { itemName: "から", categoryId: 8, isSeasonal: true },
    });
  });

  test("② 同じ名前がなく分類も未選択なら、分類の案内を返すこと（分類の確認は、同じ名前の確認のあと）", async () => {
    suggestItems.mockResolvedValue([karaage]);

    const actual = await confirmItemToSend({
      selectedShop: registeredShop,
      selectedItem: null,
      newItem: newItemOf("から"),
    });

    expect(actual).toEqual({
      isConfirmed: false,
      message: "分類を選んでください。",
    });
  });

  test("② 探すのに失敗したら、分類を選んでいても新しい商品として進めないこと", async () => {
    suggestItems.mockRejectedValue(new Error("fetch failed"));

    const actual = await confirmItemToSend({
      selectedShop: registeredShop,
      selectedItem: null,
      newItem: newItemOf("から揚げ棒", 4),
    });

    expect(actual).toEqual({
      isConfirmed: false,
      message:
        "商品を確認できませんでした。時間をおいて、もう一度「記録する」を押してください。",
    });
  });

  test("③ 新しい店なら探さずに、入力した商品名（整えたもの）・分類・季節限定を使うこと", async () => {
    const actual = await confirmItemToSend({
      selectedShop: newShop,
      selectedItem: null,
      newItem: newItemOf(" ラーメン　", 2, false),
    });

    expect(actual).toEqual({
      isConfirmed: true,
      item: { itemName: "ラーメン", categoryId: 2, isSeasonal: false },
    });
    expect(suggestItems).not.toHaveBeenCalled();
  });

  test("③ 新しい店で分類が未選択なら、分類の案内を返すこと", async () => {
    const actual = await confirmItemToSend({
      selectedShop: newShop,
      selectedItem: null,
      newItem: newItemOf("ラーメン"),
    });

    expect(actual).toEqual({
      isConfirmed: false,
      message: "分類を選んでください。",
    });
  });
});
