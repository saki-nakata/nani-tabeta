import { cleanup, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, test, vi } from "vitest";
import type { Category } from "@/types/master";
import type { RecordCardData } from "@/types/record";
import { RecordGrid } from "./record-grid";

const { removeRecord, push } = vi.hoisted(() => ({
  removeRecord: vi.fn(),
  push: vi.fn(),
}));
vi.mock("@/app/records/[id]/actions", () => ({ removeRecord }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

const categories: Category[] = [
  { id: 1, categoryName: "ドリンク", categoryEmoji: "🍹" },
];

function card(id: number, itemName: string): RecordCardData {
  return {
    id,
    eatenOn: "2026-11-10",
    rating: 4,
    itemName,
    categoryId: 1,
    shopName: "スタバ",
    mainPhotoPath: null,
    photoCount: 0,
    mainPhotoUrl: null,
  };
}

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe("RecordGrid", () => {
  test("onDeleted を渡さなければ（タイムラインなど）、「編集」「削除」を出さないこと", () => {
    render(
      <RecordGrid
        cards={[card(12, "抹茶ラテ"), card(9, "牛丼")]}
        categories={categories}
      />,
    );

    expect(screen.queryByRole("link", { name: "編集" })).toBeNull();
    expect(screen.queryByRole("button", { name: "削除" })).toBeNull();
  });

  test("onDeleted を渡すと、カードごとに、その記録の編集画面へのリンクと「削除」を出すこと", () => {
    render(
      <RecordGrid
        cards={[card(12, "抹茶ラテ"), card(9, "牛丼")]}
        categories={categories}
        onDeleted={vi.fn()}
      />,
    );

    const items = screen.getAllByRole("listitem");
    const editLinks = items.map((item) =>
      within(item).getByRole("link", { name: "編集" }).getAttribute("href"),
    );
    expect(editLinks).toEqual(["/records/12/edit", "/records/9/edit"]);
    expect(screen.getAllByRole("button", { name: "削除" })).toHaveLength(2);
  });

  test("「編集」「削除」は、カードのリンクの中に入れないこと（押したときにカードのリンクが反応しないように）", () => {
    render(
      <RecordGrid
        cards={[card(12, "抹茶ラテ")]}
        categories={categories}
        onDeleted={vi.fn()}
      />,
    );

    const cardLink = screen.getByRole("link", { name: /抹茶ラテ/ });
    expect(cardLink.getAttribute("href")).toBe("/records/12");
    expect(within(cardLink).queryByRole("button")).toBeNull();
    expect(within(cardLink).queryByRole("link", { name: "編集" })).toBeNull();
  });

  test("カードの「削除」で削除できたら、一覧へは移らず、その記録の ID で onDeleted を呼ぶこと", async () => {
    const user = userEvent.setup();
    const onDeleted = vi.fn();
    removeRecord.mockResolvedValue({ status: "success" });
    render(
      <RecordGrid
        cards={[card(12, "抹茶ラテ"), card(9, "牛丼")]}
        categories={categories}
        onDeleted={onDeleted}
      />,
    );

    const secondCard = screen.getAllByRole("listitem")[1];
    await user.click(within(secondCard).getByRole("button", { name: "削除" }));
    await user.click(screen.getByRole("button", { name: "削除する" }));

    expect(removeRecord).toHaveBeenCalledWith(9);
    await vi.waitFor(() => expect(onDeleted).toHaveBeenCalledWith(9));
    expect(push).not.toHaveBeenCalled();
  });
});
