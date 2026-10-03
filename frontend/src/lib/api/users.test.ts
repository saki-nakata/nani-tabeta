import { afterEach, describe, expect, test, vi } from "vitest";
import type { User } from "@/types/user";
import { ApiError } from "./client";
import { registerUser, searchMe } from "./users";

// 本物の ApiError は使い、apiFetch だけを偽物に差し替える
const { apiFetch } = vi.hoisted(() => ({ apiFetch: vi.fn() }));
vi.mock("./client", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./client")>()),
  apiFetch,
}));

const user: User = {
  id: "9249f1a6-72e2-404f-bd10-fa33c077575d",
  nickname: "Saki",
  bio: null,
  profilePhotoUrl: null,
  role: "user",
};

afterEach(() => {
  vi.resetAllMocks();
});

describe("searchMe", () => {
  test("登録済みの場合は自分のユーザーを返すこと", async () => {
    apiFetch.mockResolvedValue(user);

    const actual = await searchMe();

    expect(actual).toEqual(user);
    expect(apiFetch).toHaveBeenCalledWith("/api/users/me");
  });

  test("未登録（403）の場合は null を返すこと", async () => {
    apiFetch.mockRejectedValue(new ApiError(403, "Forbidden"));

    const actual = await searchMe();

    expect(actual).toBeNull();
  });

  test.each([401, 404, 500])(
    "403 以外のエラー（%i）は未登録扱いにせず、そのまま投げること",
    async (status) => {
      const expected = new ApiError(status, "エラー");
      apiFetch.mockRejectedValue(expected);

      const actual = searchMe();

      await expect(actual).rejects.toBe(expected);
    },
  );

  test("通信の失敗（Spring Boot が止まっているなど）は未登録扱いにせず、そのまま投げること", async () => {
    const expected = new TypeError("fetch failed");
    apiFetch.mockRejectedValue(expected);

    const actual = searchMe();

    await expect(actual).rejects.toBe(expected);
  });
});

describe("registerUser", () => {
  test("ニックネームを本文に入れて POST /api/users を呼ぶこと", async () => {
    apiFetch.mockResolvedValue(user);

    const actual = await registerUser("Saki");

    expect(actual).toEqual(user);
    expect(apiFetch).toHaveBeenCalledWith("/api/users", {
      method: "POST",
      body: { nickname: "Saki" },
    });
  });
});
