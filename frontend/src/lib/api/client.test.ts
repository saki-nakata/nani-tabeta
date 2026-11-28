import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { ApiError, apiFetch, publicApiFetch } from "./client";

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

describe("publicApiFetch", () => {
  // JWT を付けずに呼んだときの、fetch に渡す指定
  const withoutToken = { method: "GET", headers: {}, cache: "no-store" };
  // JWT を付けて呼んだときの、fetch に渡す指定
  const withToken = {
    method: "GET",
    headers: { Authorization: "Bearer test-token" },
    cache: "no-store",
  };
  const RECORD_URL = "http://backend.test/api/records/1";

  test("ログインしていない場合は、JWT を付けずに 1 回だけ呼び、ログインしていない人として読んだと返すこと", async () => {
    getSession.mockResolvedValue({ data: { session: null } });
    fetchMock.mockResolvedValue(Response.json({ id: 1 }));

    const actual = await publicApiFetch("/api/records/1");

    expect(actual).toEqual({ data: { id: 1 }, isGuest: true });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(RECORD_URL, withoutToken);
  });

  test("ログインしている場合は、JWT を付けて 1 回だけ呼び、ログインした人として読んだと返すこと", async () => {
    fetchMock.mockResolvedValue(Response.json({ id: 1 }));

    const actual = await publicApiFetch("/api/records/1");

    expect(actual).toEqual({ data: { id: 1 }, isGuest: false });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(RECORD_URL, withToken);
  });

  test("JWT が無効で 401 が返った場合は、JWT を外して 1 回だけ読み直し、ログインしていない人として読んだと返すこと", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(Response.json({ id: 1 }));

    const actual = await publicApiFetch("/api/records/1");

    expect(actual).toEqual({ data: { id: 1 }, isGuest: true });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock).toHaveBeenNthCalledWith(1, RECORD_URL, withToken);
    expect(fetchMock).toHaveBeenNthCalledWith(2, RECORD_URL, withoutToken);
  });

  test("読み直しも失敗した場合は、そのエラーで終わり、それ以上は読み直さないこと", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(
        Response.json(
          { message: "記録が見つかりません。id=1" },
          { status: 404 },
        ),
      );

    const actual = publicApiFetch("/api/records/1");

    await expect(actual).rejects.toEqual(
      new ApiError(404, "記録が見つかりません。id=1"),
    );
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  test("ログインしていない人として読んでも 401 が返った場合は、読み直さずに 401 のエラーで終わること", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));

    const actual = publicApiFetch("/api/records/1");

    await expect(actual).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).toHaveBeenCalledTimes(2); // JWT あり 1 回 ＋ JWT なし 1 回
  });

  test.each([403, 404, 500])(
    "JWT を付けて %i が返った場合は、読み直さずにエラーにすること",
    async (status) => {
      fetchMock.mockResolvedValue(new Response(null, { status }));

      const actual = publicApiFetch("/api/records/1");

      await expect(actual).rejects.toMatchObject({ status });
      expect(fetchMock).toHaveBeenCalledTimes(1);
    },
  );

  test("通信に失敗した場合は、読み直さずにそのエラーにすること", async () => {
    fetchMock.mockRejectedValue(new TypeError("fetch failed"));

    const actual = publicApiFetch("/api/records/1");

    await expect(actual).rejects.toThrow("fetch failed");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  test.each(["/api/areas", "/api/categories", "/api/shop-kinds"])(
    "マスタの API（%s）も呼べること",
    async (path) => {
      fetchMock.mockResolvedValue(Response.json([]));

      const actual = await publicApiFetch(path);

      expect(actual).toEqual({ data: [], isGuest: false });
    },
  );

  test.each([
    "/api/records",
    "/api/records/years",
    "/api/records/abc",
    "/api/records/1/photos",
    "/api/users/me",
    "/api/shops/suggestions",
  ])(
    "ログインしていなくても呼べる API でないパス（%s）は、API を呼ばずにエラーにすること",
    async (path) => {
      const actual = publicApiFetch(path);

      await expect(actual).rejects.toThrow(
        "ログインしていなくても呼べる API ではありません",
      );
      expect(fetchMock).not.toHaveBeenCalled();
    },
  );
});
