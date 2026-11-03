import { describe, expect, test } from "vitest";
import { dayBefore } from "./dates";

describe("dayBefore", () => {
  test("前の日を、同じ形で返すこと", () => {
    expect(dayBefore("2026-11-03")).toBe("2026-11-02");
  });

  test("月の初めは、前の月の最後の日を返すこと", () => {
    expect(dayBefore("2026-11-01")).toBe("2026-10-31");
    expect(dayBefore("2026-05-01")).toBe("2026-04-30");
  });

  test("年の初めは、前の年の 12 月 31 日を返すこと", () => {
    expect(dayBefore("2026-01-01")).toBe("2025-12-31");
  });

  test("3 月 1 日は、うるう年なら 2 月 29 日、そうでなければ 2 月 28 日を返すこと", () => {
    expect(dayBefore("2024-03-01")).toBe("2024-02-29");
    expect(dayBefore("2026-03-01")).toBe("2026-02-28");
    // 100 で割り切れる年はうるう年ではないが、400 で割り切れる年はうるう年
    expect(dayBefore("1900-03-01")).toBe("1900-02-28");
    expect(dayBefore("2000-03-01")).toBe("2000-02-29");
  });
});
