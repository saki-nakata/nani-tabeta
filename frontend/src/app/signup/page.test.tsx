import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, test, vi } from "vitest";
import SignupPage from "./page";

// 初回登録の画面（サーバーで作る部品）を作って、説明の文を確かめる
const { searchMe, redirect } = vi.hoisted(() => ({
  searchMe: vi.fn(),
  // 本物の redirect は、例外を投げて画面の処理を止める
  redirect: vi.fn(() => {
    throw new Error("NEXT_REDIRECT");
  }),
}));
vi.mock("@/lib/api/users", () => ({ searchMe }));
vi.mock("next/navigation", () => ({ redirect }));
// 入力のフォームは Server Action を使うので、ここでは中身を出さない偽物にする
vi.mock("@/components/signup-form", () => ({
  SignupForm: () => <form aria-label="ニックネームの登録" />,
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("初回登録の画面", () => {
  test("ログインが必要な画面として自分を調べ、未登録なら、名前の説明と「だれでも見られる」ことを、入力欄の前に出すこと", async () => {
    searchMe.mockResolvedValue(null);

    render(await SignupPage());

    expect(searchMe).toHaveBeenCalledWith({ allowGuest: false });
    const nameNote = screen.getByText("記録やコメントに表示される名前です。");
    const publicNote = screen.getByText(
      "記録とニックネームは、ログインしていない人も含めて、だれでも見られます。",
    );
    const form = screen.getByRole("form", { name: "ニックネームの登録" });
    // どちらの文も、入力欄より前（上）にあること
    expect(
      nameNote.compareDocumentPosition(form) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    expect(
      publicNote.compareDocumentPosition(form) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    // 「だれでも見られる」は、読み飛ばされないよう、うすい色（補足の色）にしない
    expect(publicNote.className).not.toContain("text-muted-foreground");
  });

  test("登録ずみなら、トップ（/）へ移すこと", async () => {
    searchMe.mockResolvedValue({ id: "u1", nickname: "saki" });

    await expect(SignupPage()).rejects.toThrow("NEXT_REDIRECT");

    expect(redirect).toHaveBeenCalledWith("/");
  });
});
