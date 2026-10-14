import { afterEach, describe, expect, test, vi } from "vitest";
import { LAST_AREA_COOKIE, saveLastAreaId } from "./last-area";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("saveLastAreaId", () => {
  test("エリアの ID を、サイト全体で 1 年間読める Cookie に保存すること", () => {
    // テストはブラウザではないので、document を偽物に差し替える
    const document = { cookie: "" };
    vi.stubGlobal("document", document);

    saveLastAreaId(20);

    expect(document.cookie).toBe(
      "lastAreaId=20; path=/; max-age=31536000; samesite=lax",
    );
  });

  test("Cookie の名前は、page.tsx で読む名前と同じ lastAreaId であること", () => {
    expect(LAST_AREA_COOKIE).toBe("lastAreaId");
  });
});
