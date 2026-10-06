import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { deletePhotos, uploadPhoto, uploadPhotos } from "./upload";

// ブラウザでしか動かない圧縮と、Supabase の窓口を偽物に差し替える
const { compressToWebp, getClaims, upload, remove, from } = vi.hoisted(() => {
  const upload = vi.fn();
  const remove = vi.fn();
  return {
    compressToWebp: vi.fn(),
    getClaims: vi.fn(),
    upload,
    remove,
    from: vi.fn(() => ({ upload, remove })),
  };
});
vi.mock("@/lib/photos/compress", () => ({ compressToWebp }));
vi.mock("@/lib/supabase/client", () => ({
  createClient: () => ({ auth: { getClaims }, storage: { from } }),
}));

const SUB = "9249f1a6-72e2-404f-bd10-fa33c077575d";

function fileNamed(name: string) {
  return new File(["photo"], name, { type: "image/jpeg" });
}

beforeEach(() => {
  getClaims.mockResolvedValue({ data: { claims: { sub: SUB } } });
  upload.mockResolvedValue({ error: null });
  // 頼まれた場所を、すべて消せたものとして返す
  remove.mockImplementation(async (paths: string[]) => ({
    data: paths.map((name) => ({ name })),
    error: null,
  }));
  // 後片付けの記録（ログ）を、テストの出力に出さずに確かめられるようにする
  vi.spyOn(console, "warn").mockImplementation(() => {});
  // 小さくした結果として、元のファイル名を中身に持つ WebP を返す（どの写真か見分けるため）
  compressToWebp.mockImplementation(
    async (file: File) => new Blob([file.name], { type: "image/webp" }),
  );
  let count = 0;
  vi.spyOn(crypto, "randomUUID").mockImplementation(
    () => `00000000-0000-0000-0000-00000000000${++count}` as const,
  );
});

afterEach(() => {
  vi.resetAllMocks();
  vi.restoreAllMocks();
});

describe("uploadPhoto", () => {
  test("自分の sub のフォルダに、重ならない名前の WebP として置き、置いた場所を返すこと", async () => {
    const photo = new Blob(["webp"], { type: "image/webp" });

    const actual = await uploadPhoto(photo, "records");

    const expected = `${SUB}/records/00000000-0000-0000-0000-000000000001.webp`;
    expect(actual).toBe(expected);
    expect(from).toHaveBeenCalledWith("photos");
    expect(upload).toHaveBeenCalledWith(expected, photo, {
      contentType: "image/webp",
    });
  });

  test("ログインしていない場合は、置かずにエラーを投げること", async () => {
    getClaims.mockResolvedValue({ data: null });

    const actual = uploadPhoto(new Blob(["webp"]), "records");

    await expect(actual).rejects.toThrow("ログインしていません。");
    expect(upload).not.toHaveBeenCalled();
  });

  test("置けなかった場合は、利用者向けのメッセージでエラーを投げること", async () => {
    upload.mockResolvedValue({ error: new Error("new row violates policy") });

    const actual = uploadPhoto(new Blob(["webp"]), "records");

    await expect(actual).rejects.toThrow(
      "写真をアップロードできませんでした。",
    );
  });

  test("1MB を超える場合は、ログインの確認も送信もせずに、容量が大きすぎるとエラーを投げること", async () => {
    const photo = new Blob([new Uint8Array(1024 * 1024 + 1)], {
      type: "image/webp",
    });

    const actual = uploadPhoto(photo, "records");

    await expect(actual).rejects.toThrow(
      "写真の容量が大きすぎてアップロードできませんでした。別の写真を選んでください。",
    );
    expect(getClaims).not.toHaveBeenCalled();
    expect(upload).not.toHaveBeenCalled();
  });

  test("ちょうど 1MB の場合は、置けること", async () => {
    const photo = new Blob([new Uint8Array(1024 * 1024)], {
      type: "image/webp",
    });

    const actual = await uploadPhoto(photo, "records");

    expect(actual).toBe(
      `${SUB}/records/00000000-0000-0000-0000-000000000001.webp`,
    );
  });
});

describe("uploadPhotos", () => {
  test("記録の写真は長い辺 1200px に縮め、選んだ順に置いて、その順で場所を返すこと", async () => {
    const files = [fileNamed("a.jpg"), fileNamed("b.jpg"), fileNamed("c.jpg")];

    const actual = await uploadPhotos(files, "records");

    expect(actual).toEqual([
      `${SUB}/records/00000000-0000-0000-0000-000000000001.webp`,
      `${SUB}/records/00000000-0000-0000-0000-000000000002.webp`,
      `${SUB}/records/00000000-0000-0000-0000-000000000003.webp`,
    ]);
    expect(compressToWebp.mock.calls).toEqual([
      [files[0], 1200],
      [files[1], 1200],
      [files[2], 1200],
    ]);
    // 置いた中身の順番も、選んだ順と同じであること
    const uploadedNames = await Promise.all(
      upload.mock.calls.map((call) => (call[1] as Blob).text()),
    );
    expect(uploadedNames).toEqual(["a.jpg", "b.jpg", "c.jpg"]);
    expect(remove).not.toHaveBeenCalled();
  });

  test("店の写真は長い辺 1600px に縮め、shops フォルダに置くこと", async () => {
    const actual = await uploadPhotos([fileNamed("menu.jpg")], "shops");

    expect(actual).toEqual([
      `${SUB}/shops/00000000-0000-0000-0000-000000000001.webp`,
    ]);
    expect(compressToWebp).toHaveBeenCalledWith(expect.any(File), 1600);
  });

  test("途中で置けなかった場合は、それまでに置いた分を消してからエラーを投げ、残りは置かないこと", async () => {
    upload
      .mockResolvedValueOnce({ error: null })
      .mockResolvedValueOnce({ error: new Error("network") });
    const files = [fileNamed("a.jpg"), fileNamed("b.jpg"), fileNamed("c.jpg")];

    const actual = uploadPhotos(files, "records");

    await expect(actual).rejects.toThrow(
      "写真をアップロードできませんでした。",
    );
    expect(remove).toHaveBeenCalledWith([
      `${SUB}/records/00000000-0000-0000-0000-000000000001.webp`,
    ]);
    expect(upload).toHaveBeenCalledTimes(2);
  });

  test("途中で縮められなかった場合も、それまでに置いた分を消してからエラーを投げること", async () => {
    const expected = new Error("この端末では写真を変換できませんでした。");
    compressToWebp
      .mockResolvedValueOnce(new Blob(["a"], { type: "image/webp" }))
      .mockRejectedValueOnce(expected);

    const actual = uploadPhotos(
      [fileNamed("a.jpg"), fileNamed("b.jpg")],
      "records",
    );

    await expect(actual).rejects.toBe(expected);
    expect(remove).toHaveBeenCalledWith([
      `${SUB}/records/00000000-0000-0000-0000-000000000001.webp`,
    ]);
  });

  test("後片付けの削除まで失敗しても、元のエラー（置けなかった理由）をそのまま投げること", async () => {
    upload
      .mockResolvedValueOnce({ error: null })
      .mockResolvedValueOnce({ error: new Error("network") });
    remove.mockRejectedValue(new TypeError("Failed to fetch"));

    const actual = uploadPhotos(
      [fileNamed("a.jpg"), fileNamed("b.jpg")],
      "records",
    );

    await expect(actual).rejects.toThrow(
      "写真をアップロードできませんでした。",
    );
  });

  test("1枚目で失敗した場合は、消すものがないので削除を呼ばないこと", async () => {
    upload.mockResolvedValue({ error: new Error("network") });

    const actual = uploadPhotos([fileNamed("a.jpg")], "records");

    await expect(actual).rejects.toThrow(
      "写真をアップロードできませんでした。",
    );
    expect(remove).not.toHaveBeenCalled();
  });
});

describe("deletePhotos", () => {
  test("渡された場所の写真を photos バケットから消すこと", async () => {
    const paths = [`${SUB}/records/a.webp`, `${SUB}/records/b.webp`];

    await deletePhotos(paths);

    expect(from).toHaveBeenCalledWith("photos");
    expect(remove).toHaveBeenCalledWith(paths);
    expect(console.warn).not.toHaveBeenCalled();
  });

  test("消えたことを確かめられなかった写真があれば、その場所を記録に残すこと", async () => {
    const paths = [`${SUB}/records/a.webp`, `${SUB}/records/b.webp`];
    // 権限のルールで弾かれたときと同じく、エラーなしで 1枚だけ消せたと返ってくる場合
    remove.mockResolvedValue({ data: [{ name: paths[0] }], error: null });

    await deletePhotos(paths);

    expect(console.warn).toHaveBeenCalledWith(
      "写真の削除を確認できませんでした。",
      [paths[1]],
      null,
    );
  });

  test("Supabase がエラーを返した場合は、投げずに全部の場所を記録に残すこと", async () => {
    const paths = [`${SUB}/records/a.webp`];
    const error = new Error("storage error");
    remove.mockResolvedValue({ data: null, error });

    await expect(deletePhotos(paths)).resolves.toBeUndefined();
    expect(console.warn).toHaveBeenCalledWith(
      "写真の削除を確認できませんでした。",
      paths,
      error,
    );
  });

  test("通信そのものが失敗して例外になっても、投げずに記録に残すこと", async () => {
    const paths = [`${SUB}/records/a.webp`];
    const error = new TypeError("Failed to fetch");
    remove.mockRejectedValue(error);

    await expect(deletePhotos(paths)).resolves.toBeUndefined();
    expect(console.warn).toHaveBeenCalledWith(
      "写真の削除を確認できませんでした。",
      paths,
      error,
    );
  });

  test("消すものがない場合は、Supabase を呼ばないこと", async () => {
    await deletePhotos([]);

    expect(remove).not.toHaveBeenCalled();
  });
});
