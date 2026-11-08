import { afterEach, describe, expect, test, vi } from "vitest";
import type { RecordDetail, RecordInput, RecordListPage } from "@/types/record";
import { ApiError } from "./client";
import {
  deleteRecord,
  registerRecord,
  searchRecord,
  searchRecordList,
  searchRecordYears,
} from "./records";

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

describe("deleteRecord", () => {
  test("記録の ID を URL に入れて DELETE /api/records/{id} を呼び、消した写真のパスだけを返すこと", async () => {
    const expected = [
      "9249f1a6-72e2-404f-bd10-fa33c077575d/records/a.webp",
      "9249f1a6-72e2-404f-bd10-fa33c077575d/records/b.webp",
    ];
    apiFetch.mockResolvedValue({ deletedPhotoPaths: expected });

    const actual = await deleteRecord(12);

    expect(actual).toEqual(expected);
    expect(apiFetch).toHaveBeenCalledWith("/api/records/12", {
      method: "DELETE",
    });
  });

  test("写真がない記録では、空の一覧を返すこと", async () => {
    apiFetch.mockResolvedValue({ deletedPhotoPaths: [] });

    const actual = await deleteRecord(12);

    expect(actual).toEqual([]);
  });

  test("失敗（他人の記録の 403 など）は、そのまま投げること（削除の結果を Server Action で分けるため）", async () => {
    const expected = new ApiError(403, "この記録を操作する権限がありません。");
    apiFetch.mockRejectedValue(expected);

    const actual = deleteRecord(12);

    await expect(actual).rejects.toBe(expected);
  });
});

describe("searchRecordList", () => {
  const allFilter = { categoryId: null, shopKindId: null, year: null };
  const noRange = { eatenFrom: null, eatenTo: null };
  const recordListPage: RecordListPage = {
    records: [
      {
        id: 13,
        eatenOn: "2026-10-25",
        rating: 3,
        itemName: "ラテ",
        categoryId: 10,
        shopName: "スタバ",
        mainPhotoPath: null,
        photoCount: 0,
      },
    ],
    nextCursor: { eatenOn: "2026-10-25", id: 13 },
  };

  test("絞り込みがすべて「すべて」で cursor がないときは、パラメータを付けずに GET /api/records を呼び、1 回分を返すこと", async () => {
    apiFetch.mockResolvedValue(recordListPage);

    const actual = await searchRecordList(allFilter, noRange, null);

    expect(actual).toEqual(recordListPage);
    expect(apiFetch).toHaveBeenCalledWith("/api/records");
  });

  test("「すべて」でない絞り込みだけをパラメータにすること", async () => {
    apiFetch.mockResolvedValue(recordListPage);

    await searchRecordList(
      { categoryId: 2, shopKindId: null, year: 2026 },
      noRange,
      null,
    );

    expect(apiFetch).toHaveBeenCalledWith(
      "/api/records?categoryId=2&year=2026",
    );
  });

  test("3 つの絞り込みをすべてパラメータにできること", async () => {
    apiFetch.mockResolvedValue(recordListPage);

    await searchRecordList(
      { categoryId: 2, shopKindId: 6, year: 2025 },
      noRange,
      null,
    );

    expect(apiFetch).toHaveBeenCalledWith(
      "/api/records?categoryId=2&shopKindId=6&year=2025",
    );
  });

  test("cursor があるときは、食べた日と記録 ID を 2 つそろえて、絞り込みのあとに付けること", async () => {
    apiFetch.mockResolvedValue(recordListPage);

    await searchRecordList(
      { categoryId: 2, shopKindId: null, year: 2026 },
      noRange,
      { eatenOn: "2026-10-25", id: 13 },
    );

    expect(apiFetch).toHaveBeenCalledWith(
      "/api/records?categoryId=2&year=2026&cursorEatenOn=2026-10-25&cursorId=13",
    );
  });

  test("絞り込みが「すべて」で cursor だけがあるときは、cursor の 2 つだけを付けること", async () => {
    apiFetch.mockResolvedValue(recordListPage);

    await searchRecordList(allFilter, noRange, {
      eatenOn: "2026-09-26",
      id: 2,
    });

    expect(apiFetch).toHaveBeenCalledWith(
      "/api/records?cursorEatenOn=2026-09-26&cursorId=2",
    );
  });

  test("読めなかった（401 など）ときは、そのまま投げること（0 件として扱わないため）", async () => {
    const expected = new ApiError(401, "ログインしていません。");
    apiFetch.mockRejectedValue(expected);

    const actual = searchRecordList(allFilter, noRange, null);

    await expect(actual).rejects.toBe(expected);
  });

  test("範囲（今日だけ）は、絞り込みのあと・cursor の前に、eatenFrom と eatenTo を付けること", async () => {
    apiFetch.mockResolvedValue(recordListPage);

    await searchRecordList(
      { categoryId: 2, shopKindId: null, year: null },
      { eatenFrom: "2026-11-03", eatenTo: "2026-11-03" },
      { eatenOn: "2026-11-03", id: 30 },
    );

    expect(apiFetch).toHaveBeenCalledWith(
      "/api/records?categoryId=2&eatenFrom=2026-11-03&eatenTo=2026-11-03&cursorEatenOn=2026-11-03&cursorId=30",
    );
  });

  test("範囲の片側（昨日まで）だけのときは、eatenTo だけを付けること", async () => {
    apiFetch.mockResolvedValue(recordListPage);

    await searchRecordList(
      allFilter,
      { eatenFrom: null, eatenTo: "2026-11-02" },
      null,
    );

    expect(apiFetch).toHaveBeenCalledWith("/api/records?eatenTo=2026-11-02");
  });
});

describe("searchRecordYears", () => {
  test("GET /api/records/years を呼び、年の一覧を返すこと", async () => {
    apiFetch.mockResolvedValue([2026, 2025]);

    const actual = await searchRecordYears();

    expect(actual).toEqual([2026, 2025]);
    expect(apiFetch).toHaveBeenCalledWith("/api/records/years");
  });
});
