import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, test, vi } from "vitest";
import { CancelEditButton } from "./cancel-edit-button";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe("CancelEditButton", () => {
  test("何も変えていなければ、確かめずにすぐ詳細へ戻ること", async () => {
    const user = userEvent.setup();
    render(<CancelEditButton href="/records/12" isChanged={false} />);

    await user.click(screen.getByRole("button", { name: "キャンセル" }));

    expect(screen.queryByRole("alertdialog")).toBeNull();
    expect(push).toHaveBeenCalledWith("/records/12");
  });

  test("変えていれば、「保存せずに戻りますか？」と確かめ、まだ戻らないこと", async () => {
    const user = userEvent.setup();
    render(<CancelEditButton href="/records/12" isChanged={true} />);

    await user.click(screen.getByRole("button", { name: "キャンセル" }));

    expect(
      screen.getByRole("alertdialog", { name: "保存せずに戻りますか？" }),
    ).toBeDefined();
    expect(push).not.toHaveBeenCalled();
  });

  test("確かめの小窓で「編集を続ける」を押すと、小窓を閉じるだけで戻らないこと", async () => {
    const user = userEvent.setup();
    render(<CancelEditButton href="/records/12" isChanged={true} />);

    await user.click(screen.getByRole("button", { name: "キャンセル" }));
    await user.click(screen.getByRole("button", { name: "編集を続ける" }));

    expect(screen.queryByRole("alertdialog")).toBeNull();
    expect(push).not.toHaveBeenCalled();
  });

  test("確かめの小窓で「保存せずに戻る」を押すと、詳細へ戻ること", async () => {
    const user = userEvent.setup();
    render(<CancelEditButton href="/records/12" isChanged={true} />);

    await user.click(screen.getByRole("button", { name: "キャンセル" }));
    await user.click(screen.getByRole("button", { name: "保存せずに戻る" }));

    expect(push).toHaveBeenCalledWith("/records/12");
  });
});
