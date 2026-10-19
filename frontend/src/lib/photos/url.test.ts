import { afterEach, describe, expect, test, vi } from "vitest";
import { getPhotoUrls } from "./url";

// Supabase の窓口を偽物にして、どのバケット・どの場所で URL を作ったかを確かめる
const { createClient, from, getPublicUrl } = vi.hoisted(() => {
  const getPublicUrl = vi.fn((path: string) => ({
    data: { publicUrl: `https://example.supabase.co/public/photos/${path}` },
  }));
  const from = vi.fn(() => ({ getPublicUrl }));
  const createClient = vi.fn(async () => ({ storage: { from } }));
  return { createClient, from, getPublicUrl };
});
vi.mock("@/lib/supabase/server", () => ({ createClient }));

afterEach(() => {
  vi.clearAllMocks();
});

describe("getPhotoUrls", () => {
  test("写真の場所を、選んだ順のまま公開 URL に変えること", async () => {
    const actual = await getPhotoUrls([
      "u1/records/a.webp",
      "u1/records/b.webp",
    ]);

    expect(actual).toEqual([
      "https://example.supabase.co/public/photos/u1/records/a.webp",
      "https://example.supabase.co/public/photos/u1/records/b.webp",
    ]);
    expect(getPublicUrl).toHaveBeenNthCalledWith(1, "u1/records/a.webp");
    expect(getPublicUrl).toHaveBeenNthCalledWith(2, "u1/records/b.webp");
  });

  test("写真を置いたときと同じ photos バケットを使うこと", async () => {
    await getPhotoUrls(["u1/records/a.webp"]);

    expect(from).toHaveBeenCalledWith("photos");
  });

  test("写真が何枚あっても、Supabase の窓口の用意は 1 回だけであること", async () => {
    await getPhotoUrls(["a.webp", "b.webp", "c.webp"]);

    expect(createClient).toHaveBeenCalledTimes(1);
  });

  test("写真がなければ、空の一覧を返すこと", async () => {
    const actual = await getPhotoUrls([]);

    expect(actual).toEqual([]);
    expect(getPublicUrl).not.toHaveBeenCalled();
  });
});
