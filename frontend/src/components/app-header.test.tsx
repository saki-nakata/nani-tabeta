import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, test, vi } from "vitest";
import { AppHeader } from "./app-header";

// ログアウトのボタンは Supabase とページの移動を使うので、ここでは「出ているか」だけがわかる偽物にする
vi.mock("@/components/logout-button", () => ({
  LogoutButton: () => <button type="button">ログアウト</button>,
}));

afterEach(() => {
  cleanup();
});

describe("AppHeader", () => {
  test("アプリ名は、だれにでもトップ（/）へのリンクで出すこと", () => {
    render(<AppHeader isGuest={true} />);

    expect(
      screen
        .getByRole("link", { name: "今日なに食べた？" })
        .getAttribute("href"),
    ).toBe("/");
  });

  test("登録ずみの人には「＋ 記録する」と「ログアウト」を出し、「ログイン・登録」は出さないこと", () => {
    render(<AppHeader isGuest={false} />);

    expect(
      screen.getByRole("link", { name: "＋ 記録する" }).getAttribute("href"),
    ).toBe("/records/new");
    expect(screen.getByRole("button", { name: "ログアウト" })).toBeDefined();
    expect(screen.queryByRole("link", { name: "ログイン・登録" })).toBeNull();
  });

  test("ゲストには「ログイン・登録」をトップ（/）へのリンクで出し、「＋ 記録する」と「ログアウト」は出さないこと", () => {
    // トップへ行くと、ログインしていない人は /login へ、未登録の人は /signup へ移る
    render(<AppHeader isGuest={true} />);

    expect(
      screen.getByRole("link", { name: "ログイン・登録" }).getAttribute("href"),
    ).toBe("/");
    expect(screen.queryByRole("link", { name: "＋ 記録する" })).toBeNull();
    expect(screen.queryByRole("button", { name: "ログアウト" })).toBeNull();
  });
});
