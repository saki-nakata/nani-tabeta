import { existsSync } from "node:fs";
import path from "node:path";
import { cleanup, fireEvent, render } from "@testing-library/react";
import { afterEach, describe, expect, test } from "vitest";
import { TwemojiImage, twemojiFileName } from "./twemoji-image";

// 同梱した画像のフォルダ（public/twemoji/17.0.3/。テストは frontend フォルダで動く）
const TWEMOJI_DIR =
  path.join(process.cwd(), "public", "twemoji", "17.0.3") + path.sep;

// 分類・業態のマスタの絵文字（backend の V2__master_data.sql と同じ）
const CATEGORY_EMOJIS = "🍚 🍝 🥩 🐟 🍞 🥗 🍲 🍨 🥨 🍹 🥄".split(" ");
const SHOP_KIND_EMOJIS = "🏪 🛒 🍱 🍽️ 🥢 🍜 🍣 🍖 ☕ 🍰 🥐 🍟 🍺 🚚 📦".split(
  " ",
);
const MASTER_EMOJIS = [...CATEGORY_EMOJIS, ...SHOP_KIND_EMOJIS];

afterEach(() => {
  cleanup();
});

describe("twemojiFileName", () => {
  test.each([
    ["1 文字の絵文字はそのまま番号にする", "☕", "2615"],
    ["異体字の印（U+FE0F）は外す", "🍽️", "1f37d"],
    [
      "つなぐ印（U+200D）がある絵文字は、そのまま番号にする",
      "👨‍🍳",
      "1f468-200d-1f373",
    ],
    [
      "つなぐ印がある絵文字は、異体字の印も外さない",
      "❤️‍🔥",
      "2764-fe0f-200d-1f525",
    ],
  ])("%s（%s → %s）", (_, emoji, expected) => {
    const actual = twemojiFileName(emoji);

    expect(actual).toBe(expected);
    // 作ったファイル名の画像が、同梱したフォルダに本当にあること
    expect(existsSync(`${TWEMOJI_DIR}${actual}.svg`)).toBe(true);
  });

  test("分類・業態のマスタの絵文字は、すべて同梱した画像があること", () => {
    const missing = MASTER_EMOJIS.filter(
      (emoji) => !existsSync(`${TWEMOJI_DIR}${twemojiFileName(emoji)}.svg`),
    );

    expect(missing).toEqual([]);
  });
});

describe("TwemojiImage", () => {
  test("同梱した画像を、読み上げない飾りの画像として出すこと", () => {
    const { container } = render(<TwemojiImage emoji="☕" />);

    const image = container.querySelector("img");
    expect(image?.getAttribute("src")).toBe("/twemoji/17.0.3/2615.svg");
    // alt が空なので、読み上げソフトは読まない（すぐ横に「カフェ」などの名前がある）
    expect(image?.getAttribute("alt")).toBe("");
    expect(image?.getAttribute("draggable")).toBe("false");
  });

  test("大きさをまわりの文字にそろえ、渡したクラスも足すこと", () => {
    const { container } = render(
      <TwemojiImage emoji="☕" className="text-4xl" />,
    );

    const classes = container.querySelector("img")?.className.split(" ");
    expect(classes).toEqual(expect.arrayContaining(["size-[1em]", "text-4xl"]));
  });

  test("画像を読めなかったら、文字の絵文字に戻し、読み上げないこと", () => {
    const { container } = render(
      <TwemojiImage emoji="☕" className="text-4xl" />,
    );

    fireEvent.error(container.querySelector("img") as HTMLImageElement);

    expect(container.querySelector("img")).toBeNull();
    const fallback = container.querySelector("span");
    expect(fallback?.textContent).toBe("☕");
    expect(fallback?.getAttribute("aria-hidden")).toBe("true");
    expect(fallback?.className).toBe("text-4xl");
  });

  test("画像を読めなかったあと、別の絵文字に変わったら、その画像はもう一度読みにいくこと", () => {
    const { container, rerender } = render(<TwemojiImage emoji="☕" />);
    fireEvent.error(container.querySelector("img") as HTMLImageElement);

    // 同じ部品のまま、絵文字だけが 🍹 に変わる（一覧の並びが変わったときなど）
    rerender(<TwemojiImage emoji="🍹" />);

    expect(container.querySelector("img")?.getAttribute("src")).toBe(
      "/twemoji/17.0.3/1f379.svg",
    );
    expect(container.querySelector("span")).toBeNull();
  });

  test("読めなかった絵文字に、別の絵文字をはさんで戻ったときは、同じ画像を読み直さず文字のままにすること", () => {
    const { container, rerender } = render(<TwemojiImage emoji="☕" />);
    fireEvent.error(container.querySelector("img") as HTMLImageElement);
    // 🍹 は読みにいく（失敗はしない）
    rerender(<TwemojiImage emoji="🍹" />);
    expect(container.querySelector("img")).not.toBeNull();

    rerender(<TwemojiImage emoji="☕" />);

    // 同じ URL で失敗をくり返さないように、読みにいかない
    expect(container.querySelector("img")).toBeNull();
    expect(container.querySelector("span")?.textContent).toBe("☕");
  });
});
