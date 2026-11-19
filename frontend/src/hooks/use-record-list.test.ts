import { act, renderHook } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import type { RecordCardData } from "@/types/record";
import { useRecordList } from "./use-record-list";

// 一覧を読む Server Action は、このテストでは呼ばない（最初の 1 回分を渡して始めるため）
vi.mock("@/app/actions", () => ({ loadRecordList: vi.fn() }));

function card(id: number): RecordCardData {
  return {
    id,
    eatenOn: "2026-11-10",
    rating: 4,
    itemName: `商品${id}`,
    categoryId: 1,
    shopName: "スタバ",
    mainPhotoPath: null,
    photoCount: 0,
    mainPhotoUrl: null,
  };
}

describe("useRecordList の removeDeletedCard", () => {
  test("削除した記録のカードだけを外し、ほかのカードの順番は変えないこと", () => {
    const { result } = renderHook(() =>
      useRecordList(
        {
          status: "success",
          cards: [card(15), card(12), card(9), card(7)],
          nextCursor: { eatenOn: "2026-11-01", id: 7 },
        },
        null,
        null,
      ),
    );

    act(() => result.current.removeDeletedCard(12));

    const expected = [card(15), card(9), card(7)];
    expect(result.current.cards).toEqual(expected);
    // 続きの目印（しおり）は変えない
    expect(result.current.nextCursor).toEqual({ eatenOn: "2026-11-01", id: 7 });
  });

  test("一覧にない記録の ID なら、何も変えないこと", () => {
    const { result } = renderHook(() =>
      useRecordList(
        { status: "success", cards: [card(15), card(9)], nextCursor: null },
        null,
        null,
      ),
    );

    act(() => result.current.removeDeletedCard(99));

    expect(result.current.cards).toEqual([card(15), card(9)]);
  });
});
