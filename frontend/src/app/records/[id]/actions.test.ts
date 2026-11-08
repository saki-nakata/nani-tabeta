import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { removeRecord } from "./actions";

const { deleteRecord, deletePhotosOnServer } = vi.hoisted(() => ({
  deleteRecord: vi.fn(),
  deletePhotosOnServer: vi.fn(),
}));
vi.mock("@/lib/api/records", () => ({ deleteRecord }));
vi.mock("@/lib/photos/delete-on-server", () => ({ deletePhotosOnServer }));
// 本物の ApiError を読み込む先で、Supabase の窓口（next/headers）を使わないようにする
vi.mock("@/lib/supabase/server", () => ({ createClient: vi.fn() }));

const PHOTO_PATHS = [
  "9249f1a6-72e2-404f-bd10-fa33c077575d/records/a.webp",
  "9249f1a6-72e2-404f-bd10-fa33c077575d/records/b.webp",
];

beforeEach(() => {
  // 失敗の確認でサーバーのログ（console.error）が出るので、テストの出力には出さない
  vi.spyOn(console, "error").mockImplementation(() => {});
});

afterEach(() => {
  vi.resetAllMocks();
  vi.restoreAllMocks();
});

describe("removeRecord", () => {
  test("記録を消してから、返ってきた写真を Storage から消し、成功を返すこと", async () => {
    deleteRecord.mockResolvedValue(PHOTO_PATHS);

    const actual = await removeRecord(12);

    expect(actual).toEqual({ status: "success" });
    expect(deleteRecord).toHaveBeenCalledWith(12);
    expect(deletePhotosOnServer).toHaveBeenCalledWith(PHOTO_PATHS);
    // 写真を消すのは、記録の削除が終わったあと
    expect(deleteRecord.mock.invocationCallOrder[0]).toBeLessThan(
      deletePhotosOnServer.mock.invocationCallOrder[0],
    );
  });

  test("記録の削除の返事が届くまでは、写真を消さないこと（await を外したら気づけるように）", async () => {
    // 返事をまだ返さない状態にしておき、あとで手で返す
    let resolveDeletion: (paths: string[]) => void = () => {};
    deleteRecord.mockReturnValue(
      new Promise<string[]>((resolve) => {
        resolveDeletion = resolve;
      }),
    );

    const pending = removeRecord(12);
    // 待っている間に進められる処理を、すべて進める
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(deletePhotosOnServer).not.toHaveBeenCalled();

    resolveDeletion(PHOTO_PATHS);
    const actual = await pending;

    expect(actual).toEqual({ status: "success" });
    expect(deletePhotosOnServer).toHaveBeenCalledWith(PHOTO_PATHS);
  });

  test("写真がない記録でも、成功を返すこと", async () => {
    deleteRecord.mockResolvedValue([]);

    const actual = await removeRecord(12);

    expect(actual).toEqual({ status: "success" });
    expect(deletePhotosOnServer).toHaveBeenCalledWith([]);
  });

  test("400 番台（他人の記録の 403 など）は、この頼みでは削除されていないので failed を返し、写真は消さないこと", async () => {
    deleteRecord.mockRejectedValue(
      new ApiError(403, "この記録を操作する権限がありません。"),
    );

    const actual = await removeRecord(12);

    expect(actual).toEqual({
      status: "failed",
      message: "この記録を操作する権限がありません。",
    });
    expect(deletePhotosOnServer).not.toHaveBeenCalled();
  });

  test("ログイン切れ（401）も failed を返すこと", async () => {
    deleteRecord.mockRejectedValue(new ApiError(401, "ログインしていません。"));

    const actual = await removeRecord(12);

    expect(actual).toEqual({
      status: "failed",
      message: "ログインしていません。",
    });
  });

  test("500 番台は、削除されたかわからないので unknown を返し、写真は消さずにログを残すこと", async () => {
    const error = new ApiError(500, "サーバーでエラーが発生しました。");
    deleteRecord.mockRejectedValue(error);

    const actual = await removeRecord(12);

    expect(actual).toEqual({ status: "unknown" });
    expect(deletePhotosOnServer).not.toHaveBeenCalled();
    expect(console.error).toHaveBeenCalledWith(
      "記録を削除できたか確認できませんでした。",
      { recordId: 12, error },
    );
  });

  test("通信の失敗も unknown を返し、写真は消さないこと", async () => {
    deleteRecord.mockRejectedValue(new TypeError("fetch failed"));

    const actual = await removeRecord(12);

    expect(actual).toEqual({ status: "unknown" });
    expect(deletePhotosOnServer).not.toHaveBeenCalled();
  });
});
