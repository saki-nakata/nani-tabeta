import { describe, expect, test } from "vitest";
import type { RecordCardData } from "@/types/record";
import { groupByYear } from "./year-groups";

// 年の分け方に関係する、食べた日と記録 ID だけを変えた記録
function card(id: number, eatenOn: string): RecordCardData {
  return {
    id,
    eatenOn,
    rating: 4,
    itemName: "おにぎり",
    categoryId: 1,
    shopName: "セブンイレブン",
    mainPhotoPath: null,
    photoCount: 0,
    mainPhotoUrl: null,
  };
}

describe("groupByYear", () => {
  test("食べた日の年が変わる所で区切り、並びは変えないこと", () => {
    const records = [
      card(11, "2026-10-26"),
      card(2, "2026-01-01"),
      card(14, "2025-12-31"),
      card(5, "2024-03-01"),
    ];

    const actual = groupByYear(records);

    expect(actual).toEqual([
      { year: "2026", cards: [records[0], records[1]] },
      { year: "2025", cards: [records[2]] },
      { year: "2024", cards: [records[3]] },
    ]);
  });

  test("続きを足したあとも、ページの境目で同じ年の見出しが 2 回できないこと", () => {
    const firstPage = [card(11, "2026-10-26"), card(13, "2026-10-25")];
    const nextPage = [card(12, "2026-10-25"), card(14, "2025-12-31")];

    const actual = groupByYear([...firstPage, ...nextPage]);

    expect(actual.map((group) => group.year)).toEqual(["2026", "2025"]);
    expect(actual[0].cards.map((record) => record.id)).toEqual([11, 13, 12]);
  });

  test("記録がなければ、空の配列を返すこと", () => {
    expect(groupByYear([])).toEqual([]);
  });
});
