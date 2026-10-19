import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import type { RecordInput } from "@/types/record";
import {
  registerNewShop,
  submitRecord,
  suggestItems,
  suggestShops,
} from "./actions";

const {
  searchShopSuggestions,
  registerShop,
  searchItemSuggestions,
  registerRecord,
} = vi.hoisted(() => ({
  searchShopSuggestions: vi.fn(),
  registerShop: vi.fn(),
  searchItemSuggestions: vi.fn(),
  registerRecord: vi.fn(),
}));
vi.mock("@/lib/api/shops", () => ({ searchShopSuggestions, registerShop }));
vi.mock("@/lib/api/items", () => ({ searchItemSuggestions }));
vi.mock("@/lib/api/records", () => ({ registerRecord }));
// 本物の ApiError を読み込む先で、Supabase の窓口（next/headers）を使わないようにする
vi.mock("@/lib/supabase/server", () => ({ createClient: vi.fn() }));

beforeEach(() => {
  // 失敗の確認でサーバーのログ（console.error）が出るので、テストの出力には出さない
  vi.spyOn(console, "error").mockImplementation(() => {});
});

afterEach(() => {
  vi.resetAllMocks();
  vi.restoreAllMocks();
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

describe("registerNewShop", () => {
  const input = { shopName: "一蘭", areaId: 20, shopKindId: 6 };

  test("登録できたら、成功として店を返すこと", async () => {
    const shop = { id: 9, ...input, isClosed: false };
    registerShop.mockResolvedValue(shop);

    const actual = await registerNewShop(input);

    expect(actual).toEqual({ isSuccess: true, shop });
    expect(registerShop).toHaveBeenCalledWith(input);
  });

  test.each([
    ["400 番台", new ApiError(400, "業態が存在しません。")],
    ["500 番台", new ApiError(500, "エラー")],
    ["通信の失敗", new TypeError("fetch failed")],
  ])(
    "失敗（%s）は投げずに、失敗を返すこと（案内の文は画面側で出す）",
    async (_, error) => {
      registerShop.mockRejectedValue(error);

      const actual = await registerNewShop(input);

      expect(actual).toEqual({ isSuccess: false });
    },
  );
});

describe("submitRecord", () => {
  const input: RecordInput = {
    shopId: 4,
    itemName: "からあげクン",
    categoryId: 4,
    isSeasonal: false,
    eatenOn: "2026-10-16",
    rating: 4,
    review: null,
    customNote: null,
    price: null,
    photoPaths: [],
  };

  test("登録できたら、success と記録の ID を返すこと", async () => {
    registerRecord.mockResolvedValue({ record: { id: 12 } });

    const actual = await submitRecord(input);

    expect(actual).toEqual({ status: "success", recordId: 12 });
    expect(registerRecord).toHaveBeenCalledWith(input);
  });

  test.each([400, 401, 403, 404, 499])(
    "バックエンドが %i を返したら、確実に登録されていないので failed とバックエンドの案内を返すこと",
    async (status) => {
      registerRecord.mockRejectedValue(
        new ApiError(status, "評価を選択してください。"),
      );

      const actual = await submitRecord(input);

      expect(actual).toEqual({
        status: "failed",
        message: "評価を選択してください。",
      });
    },
  );

  test.each([500, 503])(
    "バックエンドが %i を返したら、登録されたかわからないので unknown を返すこと（写真を消させない）",
    async (status) => {
      registerRecord.mockRejectedValue(new ApiError(status, "エラー"));

      const actual = await submitRecord(input);

      expect(actual).toEqual({ status: "unknown" });
    },
  );

  test("通信の失敗（ApiError ではない）は、登録されたかわからないので unknown を返すこと", async () => {
    registerRecord.mockRejectedValue(new TypeError("fetch failed"));

    const actual = await submitRecord(input);

    expect(actual).toEqual({ status: "unknown" });
  });
});
