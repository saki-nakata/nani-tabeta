import { NextRequest } from "next/server";
import { afterEach, describe, expect, test, vi } from "vitest";
import { proxy } from "./proxy";

type CookieMethods = {
  setAll: (
    cookies: { name: string; value: string; options: object }[],
    headers: Record<string, string>,
  ) => void;
};

// Supabase の窓口を偽物に差し替える。getClaims を呼んだときの動き（Cookie の書き換えと結果）をテストごとに決める
const { onGetClaims } = vi.hoisted(() => ({ onGetClaims: vi.fn() }));
vi.mock("@supabase/ssr", () => ({
  createServerClient: (
    _url: string,
    _key: string,
    options: { cookies: CookieMethods },
  ) => ({
    auth: { getClaims: async () => onGetClaims(options.cookies) },
  }),
}));

const CACHE_HEADERS = {
  "Cache-Control": "private, no-cache, no-store, must-revalidate, max-age=0",
  Expires: "0",
  Pragma: "no-cache",
};

function requestTo(path: string) {
  return new NextRequest(new URL(path, "http://localhost:3000"));
}

afterEach(() => {
  onGetClaims.mockReset();
});

describe("proxy", () => {
  test("ログインしていない場合は /login へ移動させること", async () => {
    onGetClaims.mockResolvedValue({ data: null });

    const actual = await proxy(requestTo("/records/new"));

    expect(actual.status).toBe(307);
    expect(actual.headers.get("location")).toBe("http://localhost:3000/login");
  });

  test("/login へ移動させるときも、Supabase が指示した Cookie の削除とキャッシュのヘッダーを引き継ぐこと", async () => {
    // 無効なセッションを見つけた Supabase が、Cookie の削除を指示した場合
    onGetClaims.mockImplementation(async (cookies: CookieMethods) => {
      cookies.setAll(
        [{ name: "sb-test-auth-token", value: "", options: { maxAge: 0 } }],
        CACHE_HEADERS,
      );
      return { data: null };
    });

    const actual = await proxy(requestTo("/"));

    expect(actual.headers.get("location")).toBe("http://localhost:3000/login");
    expect(actual.cookies.get("sb-test-auth-token")).toMatchObject({
      value: "",
      maxAge: 0,
    });
    expect(actual.headers.get("cache-control")).toBe(
      CACHE_HEADERS["Cache-Control"],
    );
    expect(actual.headers.get("expires")).toBe("0");
    expect(actual.headers.get("pragma")).toBe("no-cache");
  });

  test.each(["/login", "/login?error=auth", "/auth/callback?code=abc"])(
    "ログインしていなくても %s は移動させずに通すこと",
    async (path) => {
      onGetClaims.mockResolvedValue({ data: null });

      const actual = await proxy(requestTo(path));

      expect(actual.headers.get("location")).toBeNull();
      expect(actual.headers.get("x-middleware-next")).toBe("1");
    },
  );

  test("ログインしている場合は、新しくした JWT の Cookie を付けてページへ通すこと", async () => {
    onGetClaims.mockImplementation(async (cookies: CookieMethods) => {
      cookies.setAll(
        [{ name: "sb-test-auth-token", value: "new-token", options: {} }],
        CACHE_HEADERS,
      );
      return { data: { claims: { sub: "u1" } } };
    });

    const actual = await proxy(requestTo("/"));

    expect(actual.headers.get("location")).toBeNull();
    expect(actual.headers.get("x-middleware-next")).toBe("1");
    expect(actual.cookies.get("sb-test-auth-token")?.value).toBe("new-token");
    expect(actual.headers.get("cache-control")).toBe(
      CACHE_HEADERS["Cache-Control"],
    );
  });
});
