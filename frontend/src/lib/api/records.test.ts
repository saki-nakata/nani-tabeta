import { afterEach, describe, expect, test, vi } from "vitest";
import type { RecordDetail, RecordInput } from "@/types/record";
import { ApiError } from "./client";
import { registerRecord, searchRecord } from "./records";

// 本物の ApiError は使い、apiFetch だけを偽物に差し替える
const { apiFetch } = vi.hoisted(() => ({ apiFetch: vi.fn() }));
vi.mock("./client", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./client")>()),
  apiFetch,
}));

const recordDetail: RecordDetail = {
  record: {
    id: 12,
    userId: "9249f1a6-72e2-404f-bd10-fa33c077575d",
    itemId: 7,
    eatenOn: "2026-10-16",
    customNote: null,
    review: "衣がサクサクでおいしかった",
    rating: 4,
    price: 220,
  },
  item: {
    id: 7,
    shopId: 4,
    itemName: "からあげクン",
    categoryId: 4,
    isSeasonal: false,
  },
  shop: {
    id: 4,
    shopName: "スタバ",
    areaId: 20,
    shopKindId: 3,
    isClosed: false,
  },
  photoPaths: ["9249f1a6-72e2-404f-bd10-fa33c077575d/records/a.webp"],
};

afterEach(() => {
  vi.resetAllMocks();
});

describe("registerRecord", () => {
  test("登録内容を本文に入れて POST /api/records を呼び、記録の詳細を返すこと", async () => {
    apiFetch.mockResolvedValue(recordDetail);
    const input: RecordInput = {
      shopId: 4,
      itemName: "からあげクン",
      categoryId: 4,
      isSeasonal: false,
      eatenOn: "2026-10-16",
      rating: 4,
      review: "衣がサクサクでおいしかった",
      customNote: null,
      price: 220,
      photoPaths: ["9249f1a6-72e2-404f-bd10-fa33c077575d/records/a.webp"],
    };

    const actual = await registerRecord(input);

    expect(actual).toEqual(recordDetail);
    expect(apiFetch).toHaveBeenCalledWith("/api/records", {
      method: "POST",
      body: input,
    });
  });

  test("登録できなかった（400 など）ときは、そのまま投げること（写真の後片付けを呼び出し側で判断するため）", async () => {
    const expected = new ApiError(400, "評価を選択してください。");
    apiFetch.mockRejectedValue(expected);

    const actual = registerRecord({} as RecordInput);

    await expect(actual).rejects.toBe(expected);
  });
});

describe("searchRecord", () => {
  test("記録の ID を URL に入れて GET /api/records/{id} を呼び、記録の詳細を返すこと", async () => {
    apiFetch.mockResolvedValue(recordDetail);

    const actual = await searchRecord(12);

    expect(actual).toEqual(recordDetail);
    expect(apiFetch).toHaveBeenCalledWith("/api/records/12");
  });

  test("見つからない（404）ときは、そのまま投げること（詳細画面で「見つからない」を出すため）", async () => {
    const expected = new ApiError(404, "記録が見つかりません。");
    apiFetch.mockRejectedValue(expected);

    const actual = searchRecord(999);

    await expect(actual).rejects.toBe(expected);
  });
});
