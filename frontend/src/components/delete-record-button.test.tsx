import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, test, vi } from "vitest";
import { DeleteRecordButton } from "./delete-record-button";

const { removeRecord, push } = vi.hoisted(() => ({
  removeRecord: vi.fn(),
  push: vi.fn(),
}));
vi.mock("@/app/records/[id]/actions", () => ({ removeRecord }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

// 「削除」→ 小窓の「削除する」まで押す
async function confirmDelete(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole("button", { name: "削除" }));
  await user.click(screen.getByRole("button", { name: "削除する" }));
}

describe("DeleteRecordButton", () => {
  test("「削除」を押すと、確認の小窓を出すこと（まだ消さない）", async () => {
    const user = userEvent.setup();
    render(<DeleteRecordButton recordId={12} />);

    await user.click(screen.getByRole("button", { name: "削除" }));

    expect(
      screen.getByRole("alertdialog", { name: "この記録を削除しますか？" }),
    ).toBeDefined();
    expect(removeRecord).not.toHaveBeenCalled();
  });

  test("キャンセルを押すと、小窓を閉じるだけで、削除を頼まないこと", async () => {
    const user = userEvent.setup();
    render(<DeleteRecordButton recordId={12} />);

    await user.click(screen.getByRole("button", { name: "削除" }));
    await user.click(screen.getByRole("button", { name: "キャンセル" }));

    expect(screen.queryByRole("alertdialog")).toBeNull();
    expect(removeRecord).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
  });

  test("「削除する」を押すと、その記録の削除を頼み、成功したら一覧へ移ること", async () => {
    const user = userEvent.setup();
    removeRecord.mockResolvedValue({ status: "success" });
    render(<DeleteRecordButton recordId={12} />);

    await confirmDelete(user);

    expect(removeRecord).toHaveBeenCalledWith(12);
    await waitFor(() => expect(push).toHaveBeenCalledWith("/"));
  });

  test("onDeleted を渡したときは、削除できたら一覧へ移らず、onDeleted を呼ぶこと（一覧のカード）", async () => {
    const user = userEvent.setup();
    const onDeleted = vi.fn();
    removeRecord.mockResolvedValue({ status: "success" });
    render(<DeleteRecordButton recordId={12} onDeleted={onDeleted} />);

    await confirmDelete(user);

    await waitFor(() => expect(onDeleted).toHaveBeenCalledTimes(1));
    expect(push).not.toHaveBeenCalled();
  });

  test("onDeleted を渡しても、削除できなかったときは呼ばないこと", async () => {
    const user = userEvent.setup();
    const onDeleted = vi.fn();
    removeRecord.mockResolvedValue({
      status: "failed",
      message: "ログインしていません。",
    });
    render(<DeleteRecordButton recordId={12} onDeleted={onDeleted} />);

    await confirmDelete(user);

    await screen.findByRole("alert");
    expect(onDeleted).not.toHaveBeenCalled();
  });

  test("削除の処理中は、ボタンを「削除しています…」にして押せなくすること", async () => {
    const user = userEvent.setup();
    // 返事をまだ返さない状態にする
    removeRecord.mockReturnValue(new Promise(() => {}));
    render(<DeleteRecordButton recordId={12} />);

    await confirmDelete(user);

    const button = await screen.findByRole("button", {
      name: "削除しています…",
    });
    expect(button.hasAttribute("disabled")).toBe(true);
  });

  test("削除できなかった（failed）ときは、バックエンドの文を出し、もう一度押せるようにすること", async () => {
    const user = userEvent.setup();
    removeRecord.mockResolvedValue({
      status: "failed",
      message: "ログインしていません。",
    });
    render(<DeleteRecordButton recordId={12} />);

    await confirmDelete(user);

    expect((await screen.findByRole("alert")).textContent).toBe(
      "ログインしていません。",
    );
    expect(
      screen.getByRole("button", { name: "削除" }).hasAttribute("disabled"),
    ).toBe(false);
    expect(push).not.toHaveBeenCalled();
  });

  test("削除できたかわからない（unknown）ときは、一覧で確かめる案内を出すこと", async () => {
    const user = userEvent.setup();
    removeRecord.mockResolvedValue({ status: "unknown" });
    render(<DeleteRecordButton recordId={12} />);

    await confirmDelete(user);

    expect((await screen.findByRole("alert")).textContent).toBe(
      "記録を削除できたか確認できませんでした。\n記録の一覧で確かめてください。",
    );
    expect(push).not.toHaveBeenCalled();
  });

  test("Server Action の呼び出しそのものが失敗しても、一覧で確かめる案内を出し、もう一度押せるようにすること", async () => {
    const user = userEvent.setup();
    removeRecord.mockRejectedValue(new TypeError("Failed to fetch"));
    render(<DeleteRecordButton recordId={12} />);

    await confirmDelete(user);

    expect((await screen.findByRole("alert")).textContent).toContain(
      "記録を削除できたか確認できませんでした。",
    );
    expect(
      screen.getByRole("button", { name: "削除" }).hasAttribute("disabled"),
    ).toBe(false);
  });
});
