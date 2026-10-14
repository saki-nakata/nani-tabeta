import { afterEach, describe, expect, test, vi } from "vitest";
import type { Shop } from "@/types/shop";
import { registerShop, searchShopSuggestions } from "./shops";

// apiFetch だけを偽物に差し替え、どの URL・内容で呼んだかを確かめる
const { apiFetch } = vi.hoisted(() => ({ apiFetch: vi.fn() }));
vi.mock("./client", () => ({ apiFetch }));

const shop: Shop = {
  id: 4,
  shopName: "スタバ",
  areaId: 20,
  shopKindId: 3,
  isClosed: false,
};

afterEach(() => {
  vi.resetAllMocks();
});

describe("searchShopSuggestions", () => {
  test("エリアと店名を付けて GET /api/shops/suggestions を呼ぶこと", async () => {
    apiFetch.mockResolvedValue([shop]);

    const actual = await searchShopSuggestions(20, "スタ");

    expect(actual).toEqual([shop]);
    expect(apiFetch).toHaveBeenCalledWith(
      `/api/shops/suggestions?areaId=20&keyword=${encodeURIComponent("スタ")}`,
    );
  });

  test("店名が空の場合は keyword を付けないこと", async () => {
    apiFetch.mockResolvedValue([]);

    await searchShopSuggestions(20, "");

    expect(apiFetch).toHaveBeenCalledWith("/api/shops/suggestions?areaId=20");
  });

  test("店名に & や空白があっても、別の項目にならないよう変換して送ること", async () => {
    apiFetch.mockResolvedValue([]);

    await searchShopSuggestions(20, "A&B 店");

    expect(apiFetch).toHaveBeenCalledWith(
      "/api/shops/suggestions?areaId=20&keyword=A%26B+%E5%BA%97",
    );
  });
});

describe("registerShop", () => {
  test("店名・エリア・業態を本文に入れて POST /api/shops を呼ぶこと", async () => {
    apiFetch.mockResolvedValue(shop);
    const input = { shopName: "スタバ", areaId: 20, shopKindId: 3 };

    const actual = await registerShop(input);

    expect(actual).toEqual(shop);
    expect(apiFetch).toHaveBeenCalledWith("/api/shops", {
      method: "POST",
      body: input,
    });
  });
});
