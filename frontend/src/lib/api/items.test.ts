import { afterEach, describe, expect, test, vi } from "vitest";
import type { Item } from "@/types/item";
import { searchItemSuggestions } from "./items";

// apiFetch だけを偽物に差し替え、どの URL で呼んだかを確かめる
const { apiFetch } = vi.hoisted(() => ({ apiFetch: vi.fn() }));
vi.mock("./client", () => ({ apiFetch }));

const item: Item = {
  id: 7,
  shopId: 4,
  itemName: "からあげクン",
  categoryId: 4,
  isSeasonal: false,
};

afterEach(() => {
  vi.resetAllMocks();
});

describe("searchItemSuggestions", () => {
  test("店の ID を URL に入れ、商品名を付けて GET /api/shops/{id}/items を呼ぶこと", async () => {
    apiFetch.mockResolvedValue([item]);

    const actual = await searchItemSuggestions(4, "から");

    expect(actual).toEqual([item]);
    expect(apiFetch).toHaveBeenCalledWith(
      `/api/shops/4/items?keyword=${encodeURIComponent("から")}`,
    );
  });

  test("商品名が空の場合は keyword を付けないこと（その店の商品をすべて返してもらう）", async () => {
    apiFetch.mockResolvedValue([item]);

    await searchItemSuggestions(4, "");

    expect(apiFetch).toHaveBeenCalledWith("/api/shops/4/items?");
  });
});
