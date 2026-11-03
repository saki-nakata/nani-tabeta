import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import type { RecordListCard, RecordListFilter } from "@/types/record";
import { loadRecordList } from "./actions";

const { searchRecordList, getPhotoUrlsOrNull } = vi.hoisted(() => ({
  searchRecordList: vi.fn(),
  getPhotoUrlsOrNull: vi.fn(),
}));
vi.mock("@/lib/api/records", () => ({ searchRecordList }));
vi.mock("@/lib/photos/url", () => ({ getPhotoUrlsOrNull }));
// 本物の ApiError を読み込む先で、Supabase の窓口（next/headers）を使わないようにする
vi.mock("@/lib/supabase/server", () => ({ createClient: vi.fn() }));

const noRange = { eatenFrom: null, eatenTo: null };

const allFilter: RecordListFilter = {
  categoryId: null,
  shopKindId: null,
  year: null,
};

const cardWithoutPhoto: RecordListCard = {
  id: 11,
  eatenOn: "2026-10-26",
  rating: 4,
  itemName: "おにぎり",
  categoryId: 1,
  shopName: "セブンイレブン",
  mainPhotoPath: null,
  photoCount: 0,
};

const cardWithPhoto: RecordListCard = {
  id: 2,
  eatenOn: "2026-09-26",
  rating: 3,
  itemName: "からあげクン",
  categoryId: 4,
  shopName: "スタバ",
  mainPhotoPath: "u1/records/a.webp",
  photoCount: 1,
};

const photoUrl = "https://example.supabase.co/public/photos/u1/records/a.webp";

beforeEach(() => {
  // 失敗の確認でサーバーのログ（console.error）が出るので、テストの出力には出さない
  vi.spyOn(console, "error").mockImplementation(() => {});
});

afterEach(() => {
  vi.resetAllMocks();
  vi.restoreAllMocks();
});

describe("loadRecordList", () => {
  test("絞り込みと範囲と cursor をそのまま渡し、代表写真の URL を足したカードと次の cursor を返すこと", async () => {
    const filter: RecordListFilter = {
      categoryId: 2,
      shopKindId: null,
      year: 2026,
    };
    const cursor = { eatenOn: "2026-10-25", id: 13 };
    searchRecordList.mockResolvedValue({
      records: [cardWithoutPhoto, cardWithPhoto],
      nextCursor: { eatenOn: "2026-09-26", id: 2 },
    });
    getPhotoUrlsOrNull.mockResolvedValue([null, photoUrl]);

    const range = { eatenFrom: null, eatenTo: "2026-11-02" };
    const actual = await loadRecordList(filter, range, cursor);

    expect(actual).toEqual({
      status: "success",
      items: [
        { ...cardWithoutPhoto, mainPhotoUrl: null },
        { ...cardWithPhoto, mainPhotoUrl: photoUrl },
      ],
      nextCursor: { eatenOn: "2026-09-26", id: 2 },
    });
    expect(searchRecordList).toHaveBeenCalledWith(filter, range, cursor);
    // カードの順のまま、代表写真のパスを渡すこと（同じ番号どうしでカードと URL を組み合わせるため）
    expect(getPhotoUrlsOrNull).toHaveBeenCalledWith([
      null,
      "u1/records/a.webp",
    ]);
  });

  test("続きがないときは、nextCursor を null のまま返すこと", async () => {
    searchRecordList.mockResolvedValue({
      records: [cardWithPhoto],
      nextCursor: null,
    });
    getPhotoUrlsOrNull.mockResolvedValue([photoUrl]);

    const actual = await loadRecordList(allFilter, noRange, null);

    expect(actual).toEqual({
      status: "success",
      items: [{ ...cardWithPhoto, mainPhotoUrl: photoUrl }],
      nextCursor: null,
    });
  });

  test("記録が 0 件のときは、空のカードで成功として返すこと", async () => {
    searchRecordList.mockResolvedValue({ records: [], nextCursor: null });
    getPhotoUrlsOrNull.mockResolvedValue([]);

    const actual = await loadRecordList(allFilter, noRange, null);

    expect(actual).toEqual({ status: "success", items: [], nextCursor: null });
  });

  test("ログインの期限が切れている（401）ときは、ログインし直してほしいと失敗で返すこと", async () => {
    searchRecordList.mockRejectedValue(
      new ApiError(401, "ログインしていません。"),
    );

    const actual = await loadRecordList(allFilter, noRange, null);

    expect(actual).toEqual({
      status: "failed",
      message: "ログインの期限が切れました。もう一度ログインしてください。",
    });
  });

  test("サーバーのエラー（500）のときは、0 件ではなく失敗で返し、ログに残すこと", async () => {
    const error = new ApiError(500, "API の呼び出しに失敗しました（500）");
    searchRecordList.mockRejectedValue(error);

    const actual = await loadRecordList(allFilter, noRange, null);

    expect(actual).toEqual({
      status: "failed",
      message: "記録を読み込めませんでした。",
    });
    expect(console.error).toHaveBeenCalledWith(
      "記録の一覧を読めませんでした。",
      error,
    );
  });

  test("通信の失敗（ApiError でない例外）のときも、失敗で返すこと", async () => {
    searchRecordList.mockRejectedValue(new TypeError("fetch failed"));

    const actual = await loadRecordList(allFilter, noRange, null);

    expect(actual).toEqual({
      status: "failed",
      message: "記録を読み込めませんでした。",
    });
  });

  test("代表写真の URL を作れなかったときも、失敗で返すこと", async () => {
    searchRecordList.mockResolvedValue({
      records: [cardWithPhoto],
      nextCursor: null,
    });
    getPhotoUrlsOrNull.mockRejectedValue(new Error("cookies を読めない"));

    const actual = await loadRecordList(allFilter, noRange, null);

    expect(actual).toEqual({
      status: "failed",
      message: "記録を読み込めませんでした。",
    });
  });
});
