import { describe, expect, test } from "vitest";
import { normalizeName } from "./name-normalizer";

// バックエンドの NameNormalizerTest と同じ入力で、同じ結果になることを確かめる
describe("normalizeName", () => {
  test.each([
    ["前後の空白を取り除く", "  セブンイレブン  ", "セブンイレブン"],
    ["全角スペースを半角にする", "セブン　イレブン", "セブン イレブン"],
    ["連続する空白を 1 つにする", "セブン   イレブン", "セブン イレブン"],
    [
      "全角と半角が混ざった空白を 1 つにする",
      "セブン 　 イレブン",
      "セブン イレブン",
    ],
    ["前後の全角スペースも取り除く", "　抹茶ラテ　", "抹茶ラテ"],
    ["空白だけなら空になる", " 　 ", ""],
    ["整える必要がなければそのまま", "からあげクン", "からあげクン"],
  ])("%s", (_, name, expected) => {
    const actual = normalizeName(name);

    expect(actual).toBe(expected);
  });
});
