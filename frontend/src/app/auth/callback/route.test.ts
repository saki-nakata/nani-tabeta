import { NextRequest } from "next/server";
import { afterEach, describe, expect, test, vi } from "vitest";
import { GET } from "./route";

// Supabase のサーバー用の窓口を、code の交換結果だけを返す偽物に差し替える
const { exchangeCodeForSession } = vi.hoisted(() => ({
  exchangeCodeForSession: vi.fn(),
}));
vi.mock("@/lib/supabase/server", () => ({
  createClient: async () => ({ auth: { exchangeCodeForSession } }),
}));

function requestTo(path: string) {
  return new NextRequest(new URL(path, "http://localhost:3000"));
}

afterEach(() => {
  exchangeCodeForSession.mockReset();
});

describe("GET /auth/callback", () => {
  test("code の交換に成功した場合は、保存禁止の指定を付けてトップへ移動させること", async () => {
    exchangeCodeForSession.mockResolvedValue({ error: null });

    const actual = await GET(requestTo("/auth/callback?code=abc"));

    expect(exchangeCodeForSession).toHaveBeenCalledWith("abc");
    expect(actual.headers.get("location")).toBe("http://localhost:3000/");
    expect(actual.headers.get("cache-control")).toBe("private, no-store");
  });

  test("code の交換に失敗した場合は、保存禁止の指定を付けてログイン画面へ戻すこと", async () => {
    exchangeCodeForSession.mockResolvedValue({
      error: new Error("invalid code"),
    });

    const actual = await GET(requestTo("/auth/callback?code=abc"));

    expect(actual.headers.get("location")).toBe(
      "http://localhost:3000/login?error=auth",
    );
    expect(actual.headers.get("cache-control")).toBe("private, no-store");
  });

  test("code がない場合は、交換せずに保存禁止の指定を付けてログイン画面へ戻すこと", async () => {
    const actual = await GET(requestTo("/auth/callback"));

    expect(exchangeCodeForSession).not.toHaveBeenCalled();
    expect(actual.headers.get("location")).toBe(
      "http://localhost:3000/login?error=auth",
    );
    expect(actual.headers.get("cache-control")).toBe("private, no-store");
  });
});
