import { RecordCard } from "@/components/record-card";
import type { Category } from "@/types/master";
import type { RecordListItem } from "@/types/record";

type RecordGridProps = {
  items: RecordListItem[];
  // 分類の一覧（写真がない記録に、分類の絵文字を出すため）
  categories: Category[];
};

// 分類がわからないときの絵文字
const FALLBACK_EMOJI = "🍽️";

// 記録のカードを並べる（スマホ 2 列・タブレット 3 列・PC 5 列）
// 記録の一覧のほか、タイムライン・いいねした記録・検索の結果でも使う
export function RecordGrid({ items, categories }: RecordGridProps) {
  return (
    <ul className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-5 lg:gap-3.5">
      {items.map((item) => (
        <li key={item.id}>
          <RecordCard
            item={item}
            categoryEmoji={
              categories.find((category) => category.id === item.categoryId)
                ?.categoryEmoji ?? FALLBACK_EMOJI
            }
          />
        </li>
      ))}
    </ul>
  );
}
