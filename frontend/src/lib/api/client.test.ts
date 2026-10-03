import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { ApiError, apiFetch } from "./client";

// Supabase のサーバー用の窓口を、JWT を返すだけの偽物に差し替える
const { getSession } = vi.hoisted(() => ({ getSession: vi.fn() }));
vi.mock("@/lib/supabase/server", () => ({
  createClient: async () => ({ auth: { getSession } }),
}));

const fetchMock = vi.fn();

beforeEach(() => {
  vi.stubEnv("API_BASE_URL", "http://backend.test");
  vi.stubGlobal("fetch", fetchMock);
  getSession.mockResolvedValue({
    data: { session: { access_token: "test-token" } },
  });
});

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
  vi.resetAllMocks();
});

describe("apiFetch", () => {
  test("JWT を Bearer で付け、結果を保存しない指定で Spring Boot を呼ぶこと", async () => {
    fetchMock.mockResolvedValue(Response.json({ id: "u1" }));

    const actual = await apiFetch("/api/users", {
      method: "POST",
      body: { nickname: "Saki" },
    });

    expect(actual).toEqual({ id: "u1" });
    expect(fetchMock).toHaveBeenCalledWith("http://backend.test/api/users", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer test-token",
      },
      body: '{"nickname":"Saki"}',
      cache: "no-store",
    });
  });

  test("ログインしていない場合は Spring Boot を呼ばずに 401 の ApiError を投げること", async () => {
    getSession.mockResolvedValue({ data: { session: null } });

    const actual = apiFetch("/api/users/me");

    await expect(actual).rejects.toEqual(
      new ApiError(401, "ログインしていません。"),
    );
    expect(fetchMock).not.toHaveBeenCalled();
  });

  test("エラーの場合はバックエンドのメッセージとステータスを持つ ApiError を投げること", async () => {
    fetchMock.mockResolvedValue(
      Response.json(
        { statusValue: 400, message: "ニックネームを入力してください。" },
        { status: 400 },
      ),
    );

    const actual = apiFetch("/api/users", { method: "POST", body: {} });

    await expect(actual).rejects.toMatchObject({
      status: 400,
      message: "ニックネームを入力してください。",
    });
  });

  test("エラーの本文が空の場合はステータスを含む決まった文の ApiError を投げること", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 403 }));

    const actual = apiFetch("/api/users/me");

    await expect(actual).rejects.toMatchObject({
      status: 403,
      message: "API の呼び出しに失敗しました（403）",
    });
  });
});
