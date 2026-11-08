import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { deletePhotosOnServer } from "./delete-on-server";

// サーバー用の Supabase の窓口（Cookie を読む）を偽物に差し替える
const { remove, from, createClient } = vi.hoisted(() => {
  const remove = vi.fn();
  const from = vi.fn(() => ({ remove }));
  return {
    remove,
    from,
    createClient: vi.fn(async () => ({ storage: { from } })),
  };
});
vi.mock("@/lib/supabase/server", () => ({ createClient }));

const SUB = "9249f1a6-72e2-404f-bd10-fa33c077575d";
const LOG_MESSAGE = "写真を Storage から削除できたか確認できませんでした。";

beforeEach(() => {
  // 頼まれた場所を、すべて消せたものとして返す
  remove.mockImplementation(async (paths: string[]) => ({
    data: paths.map((name) => ({ name })),
    error: null,
  }));
  // 回収の手がかりのログ（console.error）を、テストの出力に出さずに確かめられるようにする
  vi.spyOn(console, "error").mockImplementation(() => {});
});

afterEach(() => {
  vi.resetAllMocks();
  vi.restoreAllMocks();
});

describe("deletePhotosOnServer", () => {
  test("渡された場所の写真を photos バケットから消し、ログは残さないこと", async () => {
    const paths = [`${SUB}/records/a.webp`, `${SUB}/records/b.webp`];

    await deletePhotosOnServer(paths);

    expect(from).toHaveBeenCalledWith("photos");
    expect(remove).toHaveBeenCalledWith(paths);
    expect(console.error).not.toHaveBeenCalled();
  });

  test("写真がなければ、Supabase を呼ばないこと", async () => {
    await deletePhotosOnServer([]);

    expect(createClient).not.toHaveBeenCalled();
    expect(remove).not.toHaveBeenCalled();
  });

  test("消えたことを確かめられなかった写真があれば、その場所をサーバーのログに残すこと", async () => {
    const paths = [`${SUB}/records/a.webp`, `${SUB}/records/b.webp`];
    // 権限のルールで弾かれたときと同じく、エラーなしで 1枚だけ消せたと返ってくる場合
    remove.mockResolvedValue({ data: [{ name: paths[0] }], error: null });

    await deletePhotosOnServer(paths);

    expect(console.error).toHaveBeenCalledWith(LOG_MESSAGE, {
      remaining: [paths[1]],
      error: null,
    });
  });

  test("Supabase がエラーを返した場合は、投げずに全部の場所をログに残すこと", async () => {
    const paths = [`${SUB}/records/a.webp`];
    const error = new Error("storage error");
    remove.mockResolvedValue({ data: null, error });

    await expect(deletePhotosOnServer(paths)).resolves.toBeUndefined();
    expect(console.error).toHaveBeenCalledWith(LOG_MESSAGE, {
      remaining: paths,
      error,
    });
  });

  test("Supabase の呼び出しそのものが失敗しても、投げずに全部の場所をログに残すこと", async () => {
    const paths = [`${SUB}/records/a.webp`];
    const error = new Error("fetch failed");
    remove.mockRejectedValue(error);

    await expect(deletePhotosOnServer(paths)).resolves.toBeUndefined();
    expect(console.error).toHaveBeenCalledWith(LOG_MESSAGE, {
      remaining: paths,
      error,
    });
  });
});
