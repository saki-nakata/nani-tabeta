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

describe("searchMe（ログインが必要な画面：allowGuest が false）", () => {
  test("登録済みの場合は自分のユーザーを返すこと", async () => {
    apiFetch.mockResolvedValue(user);

    const actual = await searchMe({ allowGuest: false });

    expect(actual).toEqual(user);
    expect(apiFetch).toHaveBeenCalledWith("/api/users/me");
  });

  test("未登録（403）の場合は null を返すこと", async () => {
    apiFetch.mockRejectedValue(new ApiError(403, "Forbidden"));

    const actual = await searchMe({ allowGuest: false });

    expect(actual).toBeNull();
  });

  test.each([401, 404, 500])(
    "403 以外のエラー（%i）は未登録扱いにせず、そのまま投げること",
    async (status) => {
      const expected = new ApiError(status, "エラー");
      apiFetch.mockRejectedValue(expected);

      const actual = searchMe({ allowGuest: false });

      await expect(actual).rejects.toBe(expected);
    },
  );

  test("通信の失敗（Spring Boot が止まっているなど）は未登録扱いにせず、そのまま投げること", async () => {
    const expected = new TypeError("fetch failed");
    apiFetch.mockRejectedValue(expected);

    const actual = searchMe({ allowGuest: false });

    await expect(actual).rejects.toBe(expected);
  });
});

describe("searchMe（だれでも見られる画面：allowGuest が true）", () => {
  test("登録済みの場合は自分のユーザーを返すこと", async () => {
    apiFetch.mockResolvedValue(user);

    const actual = await searchMe({ allowGuest: true });

    expect(actual).toEqual(user);
    expect(apiFetch).toHaveBeenCalledWith("/api/users/me");
  });

  test.each([
    ["ログインしていない・JWT が無効（401）", 401],
    ["未登録（403）", 403],
  ])("%s の場合は、ゲストとして null を返すこと", async (_, status) => {
    apiFetch.mockRejectedValue(new ApiError(status, "エラー"));

    const actual = await searchMe({ allowGuest: true });

    expect(actual).toBeNull();
  });

  test.each([404, 500])(
    "401・403 以外のエラー（%i）はゲスト扱いにせず、そのまま投げること",
    async (status) => {
      const expected = new ApiError(status, "エラー");
      apiFetch.mockRejectedValue(expected);

      const actual = searchMe({ allowGuest: true });

      await expect(actual).rejects.toBe(expected);
    },
  );

  test("通信の失敗はゲスト扱いにせず、そのまま投げること", async () => {
    const expected = new TypeError("fetch failed");
    apiFetch.mockRejectedValue(expected);

    const actual = searchMe({ allowGuest: true });

    await expect(actual).rejects.toBe(expected);
  });
});

describe("searchMe（allowGuest による 401 の扱いのちがい）", () => {
  test("同じ 401 でも、allowGuest が false なら投げ、true なら null を返すこと", async () => {
    const unauthorized = new ApiError(401, "ログインしていません。");
    apiFetch.mockRejectedValue(unauthorized);

    const loginRequired = searchMe({ allowGuest: false });
    const guestAllowed = await searchMe({ allowGuest: true });

    await expect(loginRequired).rejects.toBe(unauthorized);
    expect(guestAllowed).toBeNull();
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
