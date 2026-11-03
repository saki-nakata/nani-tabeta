import { describe, expect, test } from "vitest";
import { ALL_FILTER, isFiltered } from "./record-filter";

describe("isFiltered", () => {
  test("全部「すべて」なら、絞り込んでいないこと", () => {
    expect(isFiltered(ALL_FILTER)).toBe(false);
  });

  test("分類・業態・年のどれか 1 つでも選んでいれば、絞り込んでいること", () => {
    expect(isFiltered({ ...ALL_FILTER, categoryId: 2 })).toBe(true);
    expect(isFiltered({ ...ALL_FILTER, shopKindId: 6 })).toBe(true);
    expect(isFiltered({ ...ALL_FILTER, year: 2025 })).toBe(true);
  });
});
