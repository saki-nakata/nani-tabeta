import { afterEach, describe, expect, test, vi } from "vitest";
import { suggestItems, suggestShops } from "./actions";

const { searchShopSuggestions, searchItemSuggestions } = vi.hoisted(() => ({
  searchShopSuggestions: vi.fn(),
  searchItemSuggestions: vi.fn(),
}));
vi.mock("@/lib/api/shops", () => ({ searchShopSuggestions }));
vi.mock("@/lib/api/items", () => ({ searchItemSuggestions }));

afterEach(() => {
  vi.resetAllMocks();
});

describe("suggestShops", () => {
  test("エリアと店名をそのまま渡し、候補をそのまま返すこと", async () => {
    const expected = [{ id: 4, shopName: "スタバ" }];
    searchShopSuggestions.mockResolvedValue(expected);

    const actual = await suggestShops(20, "スタ");

    expect(actual).toBe(expected);
    expect(searchShopSuggestions).toHaveBeenCalledWith(20, "スタ");
  });

  test("失敗はそのまま投げること（画面側で「候補を探せませんでした」を出すため）", async () => {
    const expected = new Error("fetch failed");
    searchShopSuggestions.mockRejectedValue(expected);

    const actual = suggestShops(20, "スタ");

    await expect(actual).rejects.toBe(expected);
  });
});

describe("suggestItems", () => {
  test("店の ID と商品名をそのまま渡し、候補をそのまま返すこと", async () => {
    const expected = [{ id: 7, itemName: "からあげクン" }];
    searchItemSuggestions.mockResolvedValue(expected);

    const actual = await suggestItems(4, "から");

    expect(actual).toBe(expected);
    expect(searchItemSuggestions).toHaveBeenCalledWith(4, "から");
  });

  test("失敗はそのまま投げること（画面側で「候補を探せませんでした」を出すため）", async () => {
    const expected = new Error("fetch failed");
    searchItemSuggestions.mockRejectedValue(expected);

    const actual = suggestItems(4, "から");

    await expect(actual).rejects.toBe(expected);
  });
});
