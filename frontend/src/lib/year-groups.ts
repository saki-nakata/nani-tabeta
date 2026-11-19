import type { RecordCardData } from "@/types/record";

// 同じ年に食べた記録のまとまり（年の見出しの下に並べる）
export type YearGroup = {
  year: string;
  cards: RecordCardData[];
};

// 記録を、食べた日の年が変わる所で区切る
// 読みこんだページの区切りとは関係なく、いつも全部の記録を先頭から見て分ける（同じ年の見出しが 2 回出ないように）
export function groupByYear(cards: RecordCardData[]): YearGroup[] {
  const groups: YearGroup[] = [];
  for (const card of cards) {
    const year = card.eatenOn.slice(0, 4);
    const lastGroup = groups.at(-1);
    if (lastGroup && lastGroup.year === year) {
      lastGroup.cards.push(card);
    } else {
      groups.push({ year, cards: [card] });
    }
  }
  return groups;
}
