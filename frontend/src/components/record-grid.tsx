import { RecordCard } from "@/components/record-card";
import type { Category } from "@/types/master";
import type { RecordCardData } from "@/types/record";
import { RecordCardActions } from "@/components/record-card-actions";

type RecordGridProps = {
  cards: RecordCardData[];
  // 分類の一覧（写真がない記録に、分類の絵文字を出すため）
  categories: Category[];
  // 記録を削除したとき。渡したときだけ、カードに「編集」「削除」を出す（自分の記録の一覧で使う）
  onDeleted?: (recordId: number) => void;
};

// 分類がわからないときの絵文字
const FALLBACK_EMOJI = "🍽️";

// 記録のカードを並べる（スマホ 2 列・タブレット 3 列・PC 5 列）
// 記録の一覧のほか、タイムライン・いいねした記録・検索の結果でも使う
export function RecordGrid({ cards, categories, onDeleted }: RecordGridProps) {
  return (
    <ul className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-5 lg:gap-3.5">
      {cards.map((card) => (
        <li key={card.id} className="group relative">
          <RecordCard
            card={card}
            categoryEmoji={
              categories.find((category) => category.id === card.categoryId)
                ?.categoryEmoji ?? FALLBACK_EMOJI
            }
          />
          {onDeleted && (
            <RecordCardActions
              recordId={card.id}
              onDeleted={() => onDeleted(card.id)}
            />
          )}
        </li>
      ))}
    </ul>
  );
}
