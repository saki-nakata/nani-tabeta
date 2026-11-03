import { useCallback, useRef, useState } from "react";
import { loadRecordList, type LoadRecordListResult } from "@/app/actions";
import type {
  RecordListCursor,
  RecordListFilter,
  RecordListItem,
} from "@/types/record";

// 記録の一覧の 1 つの欄（「今日」か「これまで」）の、読みこみの状態と読み方をまとめたフック
// 呼ぶたびに、カード・しおり・読み込み中・失敗・古い返事を捨てる番号を、それぞれ別に持つ
// （「今日」の欄と「これまで」の欄で 1 回ずつ呼び、2 つの欄の状態を分ける）
// initialResult：サーバーで読んだ最初の 1 回分（失敗なら、その欄に失敗を出すところから始める）
// eatenFrom・eatenTo：この欄の食べた日の範囲（"2026-11-03" の形。null の側は制限しない）
export function useRecordList(
  initialResult: LoadRecordListResult,
  eatenFrom: string | null,
  eatenTo: string | null,
) {
  const [items, setItems] = useState<RecordListItem[]>(
    initialResult.status === "success" ? initialResult.items : [],
  );
  const [nextCursor, setNextCursor] = useState<RecordListCursor | null>(
    initialResult.status === "success" ? initialResult.nextCursor : null,
  );
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(
    initialResult.status === "failed" ? initialResult.message : null,
  );
  // 読みこみ中かどうか（同じ続きを 2 回読まないための印。画面には使わないので ref にする）
  const isLoadingRef = useRef(false);
  // 何回目の読みこみか。読みはじめるたびに 1 増やし、番号がちがう（古い）返事は捨てる
  const requestIdRef = useRef(0);

  // 記録を 1 回分読む。cursor が null なら最初から読み直し、あれば今のカードのうしろに足す
  const load = useCallback(
    async (filter: RecordListFilter, cursor: RecordListCursor | null) => {
      requestIdRef.current += 1;
      const requestId = requestIdRef.current;
      isLoadingRef.current = true;
      setIsLoading(true);
      setErrorMessage(null);
      try {
        const result = await loadRecordList(
          filter,
          { eatenFrom, eatenTo },
          cursor,
        );
        // 読んでいる間に、新しい読みこみ（絞り込みの変更）が始まっていたら、この返事は捨てる
        if (requestId !== requestIdRef.current) {
          return;
        }
        if (result.status === "success") {
          setItems((stateItems) =>
            cursor === null ? result.items : [...stateItems, ...result.items],
          );
          setNextCursor(result.nextCursor);
        } else {
          // しおりは進めない（「もう一度読む」で、同じ所から読み直せるように）
          setErrorMessage(result.message);
        }
      } catch (error) {
        // サーバーに届かなかった（電波が切れた など）。古い読みこみの失敗は捨てる
        if (requestId !== requestIdRef.current) {
          return;
        }
        console.error("記録の一覧を読めませんでした。", error);
        setErrorMessage("記録を読み込めませんでした。");
      } finally {
        // 読み込み中を戻すのは、いちばん新しい読みこみだけ
        // （古い読みこみが戻すと、新しい読みこみの途中なのに、続きをもう 1 回読めてしまう）
        if (requestId === requestIdRef.current) {
          isLoadingRef.current = false;
          setIsLoading(false);
        }
      }
    },
    [eatenFrom, eatenTo],
  );

  // 絞り込みを変えたときに、カードとしおりを空にして、最初から読み直す
  const reload = useCallback(
    (filter: RecordListFilter) => {
      setItems([]);
      setNextCursor(null);
      load(filter, null);
    },
    [load],
  );

  // 続きを読む（続きがない・読みこみ中なら何もしない）
  const loadMore = useCallback(
    (filter: RecordListFilter) => {
      if (nextCursor === null || isLoadingRef.current) {
        return;
      }
      load(filter, nextCursor);
    },
    [load, nextCursor],
  );

  // 失敗した所から、もう一度読む（しおりは進めていないので、同じ所から読める）
  const retry = useCallback(
    (filter: RecordListFilter) => {
      load(filter, nextCursor);
    },
    [load, nextCursor],
  );

  return {
    items,
    nextCursor,
    isLoading,
    errorMessage,
    reload,
    loadMore,
    retry,
  };
}
