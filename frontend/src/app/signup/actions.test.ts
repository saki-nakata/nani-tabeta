import { afterEach, describe, expect, test, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { signup } from "./actions";

const { registerUser, redirect } = vi.hoisted(() => ({
  registerUser: vi.fn(),
  // 本物の redirect と同じく、例外を投げて処理を止める
  redirect: vi.fn((path: string) => {
    throw new Error(`REDIRECT:${path}`);
  }),
}));
vi.mock("@/lib/api/users", () => ({ registerUser }));
vi.mock("next/navigation", () => ({ redirect }));
// apiFetch を読み込む先で Supabase の窓口（next/headers）を使わないようにする
vi.mock("@/lib/supabase/server", () => ({ createClient: vi.fn() }));

function formDataOf(nickname: string) {
  const formData = new FormData();
  formData.set("nickname", nickname);
  return formData;
}

afterEach(() => {
  registerUser.mockReset();
  redirect.mockClear();
});

describe("signup", () => {
  test("登録できた場合は、入力したニックネームで登録してトップへ移動すること", async () => {
    registerUser.mockResolvedValue({});

    const actual = signup(null, formDataOf("Saki"));

    await expect(actual).rejects.toThrow("REDIRECT:/");
    expect(registerUser).toHaveBeenCalledWith("Saki");
  });

  test("入力内容の誤り（400）の場合は、バックエンドのメッセージを返して移動しないこと", async () => {
    registerUser.mockRejectedValue(
      new ApiError(400, "ニックネームを入力してください。"),
    );

    const actual = await signup(null, formDataOf("   "));

    expect(actual).toEqual({ message: "ニックネームを入力してください。" });
    expect(redirect).not.toHaveBeenCalled();
  });

  test("登録済み（409）の場合は、登録できたものとしてトップへ移動すること", async () => {
    registerUser.mockRejectedValue(
      new ApiError(409, "このユーザーはすでに登録されています。"),
    );

    const actual = signup(null, formDataOf("Saki"));

    await expect(actual).rejects.toThrow("REDIRECT:/");
  });

  test.each([401, 403, 500])(
    "それ以外のエラー（%i）はそのまま投げて、移動しないこと",
    async (status) => {
      const expected = new ApiError(status, "エラー");
      registerUser.mockRejectedValue(expected);

      const actual = signup(null, formDataOf("Saki"));

      await expect(actual).rejects.toBe(expected);
      expect(redirect).not.toHaveBeenCalled();
    },
  );

  test("ニックネームの入力欄がない場合は、空文字で登録を試みること", async () => {
    registerUser.mockRejectedValue(
      new ApiError(400, "ニックネームを入力してください。"),
    );

    await signup(null, new FormData());

    expect(registerUser).toHaveBeenCalledWith("");
  });
});
