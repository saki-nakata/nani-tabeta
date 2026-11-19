import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { MAX_PHOTOS, PhotoPicker, type SelectedPhoto } from "./photo-picker";

// 保存済みの写真（編集のときの今の写真）
function savedPhoto(name: string): SelectedPhoto {
  const path = `20b622c8/records/${name}.webp`;
  return { type: "saved", id: path, path, url: `https://example.com/${path}` };
}

// 新しく選んだ写真
function newPhoto(name: string): SelectedPhoto {
  const previewUrl = `blob:${name}`;
  return {
    type: "new",
    id: previewUrl,
    file: new File(["photo"], `${name}.jpg`, { type: "image/jpeg" }),
    previewUrl,
  };
}

beforeEach(() => {
  // jsdom には、プレビュー用の URL を作る・片付ける仕組みがないので、偽物にする
  let count = 0;
  URL.createObjectURL = vi.fn(() => `blob:created-${++count}`);
  URL.revokeObjectURL = vi.fn();
  // jsdom は画面の位置を測れない（全部 0 になる）ので、「写真 N枚目」を横に 110px ずつ並んでいることにする
  // （キーボードで並べかえるとき、dnd-kit が写真の位置を測って、となりの写真を探すため）
  vi.spyOn(Element.prototype, "getBoundingClientRect").mockImplementation(
    function (this: Element) {
      const alt = this.querySelector("img")?.getAttribute("alt") ?? "";
      const position = Number(alt.match(/写真 (\d+)枚目/)?.[1] ?? 0);
      const left = position === 0 ? 0 : (position - 1) * 110;
      return {
        x: left,
        y: 0,
        left,
        top: 0,
        right: left + 100,
        bottom: 100,
        width: 100,
        height: 100,
        toJSON: () => ({}),
      };
    },
  );
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("PhotoPicker", () => {
  test("写真を並んだ順に表示し、先頭にだけ「代表」を付けること", () => {
    const photos = [savedPhoto("a"), newPhoto("x")];

    render(<PhotoPicker photos={photos} onChange={vi.fn()} />);

    const images = screen.getAllByRole("img");
    expect(images.map((image) => image.getAttribute("src"))).toEqual([
      "https://example.com/20b622c8/records/a.webp",
      "blob:x",
    ]);
    expect(screen.getAllByText("代表")).toHaveLength(1);
    expect(screen.getByText("2 / 5 枚")).toBeDefined();
  });

  test("× を押すと、その写真を外した一覧を渡し、新しい写真のプレビュー用の URL を片付けること", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const photos = [savedPhoto("a"), newPhoto("x"), savedPhoto("b")];
    render(<PhotoPicker photos={photos} onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: "2枚目の写真を外す" }));

    const expected = [photos[0], photos[2]];
    expect(onChange).toHaveBeenCalledWith(expected);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:x");
  });

  test("保存済みの写真を外しても、プレビュー用の URL の片付けはしないこと", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const photos = [savedPhoto("a"), savedPhoto("b")];
    render(<PhotoPicker photos={photos} onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: "1枚目の写真を外す" }));

    expect(onChange).toHaveBeenCalledWith([photos[1]]);
    expect(URL.revokeObjectURL).not.toHaveBeenCalled();
  });

  test("写真を選ぶと、新しい写真としてうしろに足すこと（合わせて 5 枚まで）", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const photos = [savedPhoto("a"), savedPhoto("b"), savedPhoto("c")];
    const { container } = render(
      <PhotoPicker photos={photos} onChange={onChange} />,
    );
    const files = ["p", "q", "r"].map(
      (name) => new File(["photo"], `${name}.jpg`, { type: "image/jpeg" }),
    );

    const input = container.querySelector('input[type="file"]');
    if (!(input instanceof HTMLInputElement)) {
      throw new Error("写真を選ぶ欄が見つかりません");
    }
    await user.upload(input, files);

    // 残りは 2 枚分なので、3 枚選んでも先頭の 2 枚だけを足す
    const actual: SelectedPhoto[] = onChange.mock.calls[0][0];
    expect(actual).toHaveLength(MAX_PHOTOS);
    expect(actual.slice(0, 3)).toEqual(photos);
    expect(actual.slice(3)).toEqual([
      {
        type: "new",
        id: "blob:created-1",
        file: files[0],
        previewUrl: "blob:created-1",
      },
      {
        type: "new",
        id: "blob:created-2",
        file: files[1],
        previewUrl: "blob:created-2",
      },
    ]);
  });

  test("5 枚あるときは「写真を選ぶ」を出さないこと", () => {
    const photos = ["a", "b", "c", "d", "e"].map(savedPhoto);

    render(<PhotoPicker photos={photos} onChange={vi.fn()} />);

    expect(screen.queryByText("写真を選ぶ")).toBeNull();
    expect(screen.getByText("5 / 5 枚")).toBeDefined();
  });

  test("並べかえの案内は、写真が 2 枚以上のときだけ出すこと", () => {
    const { rerender } = render(
      <PhotoPicker photos={[savedPhoto("a")]} onChange={vi.fn()} />,
    );
    expect(screen.queryByText(/並べかえられます/)).toBeNull();

    rerender(
      <PhotoPicker
        photos={[savedPhoto("a"), savedPhoto("b")]}
        onChange={vi.fn()}
      />,
    );
    expect(screen.getByText(/並べかえられます/)).toBeDefined();
  });

  test("キーボードで、写真をスペースでつかみ、→ で動かし、スペースで置くと、並べかえた一覧を渡すこと", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const photos = [savedPhoto("a"), newPhoto("x"), savedPhoto("b")];
    render(<PhotoPicker photos={photos} onChange={onChange} />);

    screen.getByRole("button", { name: "1枚目の写真を動かす" }).focus();
    await user.keyboard("[Space]");
    await user.keyboard("[ArrowRight]");
    await user.keyboard("[Space]");

    const expected = [photos[1], photos[0], photos[2]];
    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expected));
  });

  test("キーボードでつかんだあと Esc を押すと、並べかえないこと", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const photos = [savedPhoto("a"), savedPhoto("b")];
    render(<PhotoPicker photos={photos} onChange={onChange} />);

    screen.getByRole("button", { name: "1枚目の写真を動かす" }).focus();
    await user.keyboard("[Space]");
    await user.keyboard("[ArrowRight]");
    await user.keyboard("[Escape]");

    expect(onChange).not.toHaveBeenCalled();
  });

  test("つかむ所（写真）は、読み上げで「並べかえできる写真」と伝え、日本語の操作の説明とつながっていること", () => {
    render(
      <PhotoPicker
        photos={[savedPhoto("a"), savedPhoto("b")]}
        onChange={vi.fn()}
      />,
    );

    const handle = screen.getByRole("button", { name: "1枚目の写真を動かす" });
    expect(handle.getAttribute("aria-roledescription")).toBe(
      "並べかえできる写真",
    );
    const describedBy = handle.getAttribute("aria-describedby");
    expect(document.getElementById(describedBy ?? "")?.textContent).toContain(
      "スペースキーでつかみ",
    );
  });
});
