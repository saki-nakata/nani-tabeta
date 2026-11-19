import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import type { RecordInput } from "@/types/record";
import { saveRecord } from "./actions";

const { updateRecord, deletePhotosOnServer } = vi.hoisted(() => ({
  updateRecord: vi.fn(),
  deletePhotosOnServer: vi.fn(),
}));
vi.mock("@/lib/api/records", () => ({ updateRecord }));
vi.mock("@/lib/photos/delete-on-server", () => ({ deletePhotosOnServer }));
// 本物の ApiError を読み込む先で、Supabase の窓口（next/headers）を使わないようにする
vi.mock("@/lib/supabase/server", () => ({ createClient: vi.fn() }));

const A = "20b622c8/records/a.webp";
const B = "20b622c8/records/b.webp";
const C = "20b622c8/records/c.webp";
const X = "20b622c8/records/x.webp";

const CONFLICT_MESSAGE =
  "ほかの画面で先に保存されています。\n詳細画面を開き直してから、もう一度編集してください。";

const input: RecordInput = {
  shopId: 4,
  itemName: "抹茶ラテ",
  categoryId: 10,
  isSeasonal: false,
  eatenOn: "2026-11-08",
  rating: 5,
  review: "濃くておいしい",
  customNote: null,
  price: null,
  photoPaths: [C, X, A],
};

/** バックエンドの編集の返事（記録の詳細は、この Server Action では使わない） */
function updateResult(removedPhotoPaths: string[]) {
  return { recordDetail: {}, removedPhotoPaths };
}

beforeEach(() => {
  // 失敗の確認でサーバーのログ（console.error）が出るので、テストの出力には出さない
  vi.spyOn(console, "error").mockImplementation(() => {});
});

afterEach(() => {
  vi.resetAllMocks();
  vi.restoreAllMocks();
});

describe("saveRecord", () => {
  test("記録 ID・入力・編集画面が読んだ版を、そのまま updateRecord に渡すこと", async () => {
    updateRecord.mockResolvedValue(updateResult([]));

    await saveRecord(12, 3, input);

    expect(updateRecord).toHaveBeenCalledWith(12, input, 3);
  });

  test("保存できたら、バックエンドが返した外した写真だけを Storage から消し、成功を返すこと", async () => {
    // 前は A・B・C。B を外し、X を足し、並べかえた → バックエンドが B を返す
    updateRecord.mockResolvedValue(updateResult([B]));

    const actual = await saveRecord(12, 3, input);

    expect(actual).toEqual({ status: "success" });
    expect(deletePhotosOnServer).toHaveBeenCalledWith([B]);
  });

  test("外した写真がなければ（並べかえただけなど）、空の一覧を渡すこと", async () => {
    updateRecord.mockResolvedValue(updateResult([]));

    const actual = await saveRecord(12, 3, input);

    expect(actual).toEqual({ status: "success" });
    expect(deletePhotosOnServer).toHaveBeenCalledWith([]);
  });

  test("消す写真は、ブラウザが送った写真の一覧からではなく、バックエンドの返事から決めること", async () => {
    // 送った一覧には A がないが、バックエンドが外したと返したのは B だけ → A は消さない
    updateRecord.mockResolvedValue(updateResult([B]));

    await saveRecord(12, 3, { ...input, photoPaths: [C] });

    expect(deletePhotosOnServer).toHaveBeenCalledWith([B]);
  });

  test("保存の前に記録を読み直さないこと（外した写真は、保存と同じ処理の中でバックエンドが決めるため）", async () => {
    updateRecord.mockResolvedValue(updateResult([]));

    await saveRecord(12, 3, input);

    // 呼ぶのは編集の 1 回だけ
    expect(updateRecord).toHaveBeenCalledTimes(1);
  });

  test("写真を消せなくても、保存は成功を返すこと（消せなかった分は deletePhotosOnServer がログに残す）", async () => {
    updateRecord.mockResolvedValue(updateResult([B]));
    // deletePhotosOnServer は投げない決まりなので、何も起きなかったように終わる
    deletePhotosOnServer.mockResolvedValue(undefined);

    const actual = await saveRecord(12, 3, input);

    expect(actual).toEqual({ status: "success" });
  });

  test("ほかの画面で先に保存されていた（409）ら、バックエンドの案内で failed を返し、写真は消さないこと", async () => {
    updateRecord.mockRejectedValue(new ApiError(409, CONFLICT_MESSAGE));

    const actual = await saveRecord(12, 3, input);

    expect(actual).toEqual({ status: "failed", message: CONFLICT_MESSAGE });
    // 先に保存した画面の写真を消してしまわないように、何も消さない
    expect(deletePhotosOnServer).not.toHaveBeenCalled();
  });

  test("保存が 400 番台（入力の誤り・他人の記録など）なら、failed を返し、写真は消さないこと", async () => {
    updateRecord.mockRejectedValue(
      new ApiError(403, "この記録を変更する権限がありません。"),
    );

    const actual = await saveRecord(12, 3, input);

    expect(actual).toEqual({
      status: "failed",
      message: "この記録を変更する権限がありません。",
    });
    expect(deletePhotosOnServer).not.toHaveBeenCalled();
  });

  test("保存が 500 番台なら、保存されたかわからないので unknown を返し、写真は消さずにログを残すこと", async () => {
    const error = new ApiError(500, "サーバーでエラーが発生しました。");
    updateRecord.mockRejectedValue(error);

    const actual = await saveRecord(12, 3, input);

    expect(actual).toEqual({ status: "unknown" });
    expect(deletePhotosOnServer).not.toHaveBeenCalled();
    expect(console.error).toHaveBeenCalledWith(
      "記録を保存できたか確認できませんでした。",
      { recordId: 12, error },
    );
  });

  test("保存の通信が失敗しても unknown を返し、写真は消さないこと", async () => {
    updateRecord.mockRejectedValue(new TypeError("fetch failed"));

    const actual = await saveRecord(12, 3, input);

    expect(actual).toEqual({ status: "unknown" });
    expect(deletePhotosOnServer).not.toHaveBeenCalled();
  });
});
