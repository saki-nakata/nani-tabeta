import { expect, test, type Locator } from "@playwright/test";

// 分類・業態の絵文字が、同梱した Twemoji の画像（public/twemoji/17.0.3/）として、本物のブラウザで読みこめることを確かめる
// 絵文字からファイル名を作る決まりや、読めなかったときに文字へ戻す動きは、Vitest（twemoji-image.test.tsx）で確かめている

// 画像を読み終わり、中身がある（壊れた画像ではない）こと
async function expectLoaded(image: Locator) {
  await expect
    .poll(() =>
      image.evaluate(
        (element: HTMLImageElement) =>
          element.complete && element.naturalWidth > 0,
      ),
    )
    .toBe(true);
}

test("記録の一覧の絞り込みで、分類・業態の絵文字が同梱した Twemoji の画像で出ること", async ({
  page,
}) => {
  await page.goto("/");

  // 分類の「すべて」は 🍽️（見えない合図 U+FE0F つき）。合図を外したファイル名で読めていること
  const categoryRow = page.getByRole("region", { name: "分類で絞り込む" });
  const categoryAll = categoryRow
    .getByRole("button", { name: "すべて" })
    .locator("img");
  await expect(categoryAll).toHaveAttribute("src", "/twemoji/17.0.3/1f37d.svg");
  await expectLoaded(categoryAll);

  // 業態の「すべて」は 🏬
  const shopKindRow = page.getByRole("region", { name: "業態で絞り込む" });
  const shopKindAll = shopKindRow
    .getByRole("button", { name: "すべて" })
    .locator("img");
  await expect(shopKindAll).toHaveAttribute("src", "/twemoji/17.0.3/1f3ec.svg");
  await expectLoaded(shopKindAll);

  // DB の分類の絵文字（いちばん左の分類）も、同梱した画像で読めていること
  const firstCategory = categoryRow
    .getByRole("button")
    .filter({ hasNotText: "すべて" })
    .first()
    .locator("img");
  await expect(firstCategory).toHaveAttribute(
    "src",
    /^\/twemoji\/17\.0\.3\/[0-9a-f-]+\.svg$/,
  );
  await expectLoaded(firstCategory);

  // 画像の帰属表示（CC-BY 4.0）がフッターに出ていること
  await expect(page.getByRole("contentinfo")).toContainText(
    "絵文字：Twemoji（CC-BY 4.0）",
  );
});
