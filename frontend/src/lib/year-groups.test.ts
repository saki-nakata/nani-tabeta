import { describe, expect, test } from "vitest";
import type { RecordListItem } from "@/types/record";
import { groupByYear } from "./year-groups";

// 年の分け方に関係する、食べた日と記録 ID だけを変えた記録
function item(id: number, eatenOn: string): RecordListItem {
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
      item(11, "2026-10-26"),
      item(2, "2026-01-01"),
      item(14, "2025-12-31"),
      item(5, "2024-03-01"),
    ];

    const actual = groupByYear(records);

    expect(actual).toEqual([
      { year: "2026", items: [records[0], records[1]] },
      { year: "2025", items: [records[2]] },
      { year: "2024", items: [records[3]] },
    ]);
  });

  test("続きを足したあとも、ページの境目で同じ年の見出しが 2 回できないこと", () => {
    const firstPage = [item(11, "2026-10-26"), item(13, "2026-10-25")];
    const nextPage = [item(12, "2026-10-25"), item(14, "2025-12-31")];

    const actual = groupByYear([...firstPage, ...nextPage]);

    expect(actual.map((group) => group.year)).toEqual(["2026", "2025"]);
    expect(actual[0].items.map((record) => record.id)).toEqual([11, 13, 12]);
  });

  test("記録がなければ、空の配列を返すこと", () => {
    expect(groupByYear([])).toEqual([]);
  });
});
