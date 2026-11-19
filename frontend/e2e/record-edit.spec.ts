import { expect, test, type Page } from "@playwright/test";

// 記録の編集の流れ（画面で入力 → 保存 → 詳細に反映 → 読み直しても残る）を、本物のブラウザ・バックエンド・DB・Storage で確かめる
// 細かい分かれ道（入力の誤り・保存の失敗など）は、Vitest・JUnit のテストで確かめている

// 詳細画面の写真の並び（Storage のパス。メインの写真と小さい写真で同じものが出るので、重ねずに並んだ順で返す）
async function photoPathsOnDetail(page: Page): Promise<string[]> {
  const sources = await page
    .locator("article img")
    .evaluateAll((images) =>
      images.map((image) => image.getAttribute("src") ?? ""),
    );
  const paths = sources
    .map(
      (source) =>
        decodeURIComponent(source).match(/records\/[\w-]+\.webp/)?.[0],
    )
    .filter((path): path is string => path !== undefined);
  return [...new Set(paths)];
}

// テストで作った記録（最後に消すため）
let createdRecordId: string | null = null;

test.afterEach(async ({ page }) => {
  if (createdRecordId === null) {
    return;
  }
  // 作った記録を、詳細画面の「削除」で消す（写真も Storage から消える）
  await page.goto(`/records/${createdRecordId}`);
  await page.getByRole("button", { name: "削除", exact: true }).click();
  await page.getByRole("button", { name: "削除する" }).click();
  await expect(page).toHaveURL("/");
  createdRecordId = null;
});

test("記録を編集して保存すると、詳細に変更が出て、読み直しても残ること", async ({
  page,
}) => {
  // テストのたびに違う感想にして、前のテストの記録とまちがえないようにする
  const runId = Date.now();
  const reviewBefore = `E2E 編集前 ${runId}`;
  const reviewAfter = `E2E 編集後 ${runId}`;

  // ① 写真 2 枚の記録を作る
  await page.goto("/records/new");
  await page
    .locator('input[type="file"]')
    .setInputFiles(["e2e/fixtures/photo-a.jpg", "e2e/fixtures/photo-b.jpg"]);
  await page.getByLabel("地方").selectOption("中部");
  await page.getByLabel("都道府県・国").selectOption("長野県");
  await page.getByRole("textbox", { name: "店名" }).fill("スタバ");
  await page.getByRole("button", { name: "スタバ", exact: true }).click();
  await page.getByRole("textbox", { name: "商品名" }).fill("ラテ");
  await page.getByRole("button", { name: "ラテ", exact: true }).click();
  await page.getByRole("button", { name: "3点" }).click();
  await page.getByRole("textbox", { name: "感想" }).fill(reviewBefore);
  await page.getByRole("button", { name: "記録する" }).click();

  await expect(page).toHaveURL(/\/records\/\d+$/, { timeout: 20_000 });
  createdRecordId = page.url().match(/(\d+)$/)?.[1] ?? null;
  await expect(page.getByText(reviewBefore)).toBeVisible();
  const photosBefore = await photoPathsOnDetail(page);
  expect(photosBefore).toHaveLength(2);

  // ② 詳細の「編集」から編集画面へ
  await page.getByRole("link", { name: "編集" }).click();
  await expect(page).toHaveURL(`/records/${createdRecordId}/edit`);
  await expect(page.getByRole("textbox", { name: "感想" })).toHaveValue(
    reviewBefore,
  );

  // ③ 感想・評価を変え、2 枚目の写真を先頭へ動かす（キーボードで並べかえる）
  await page.getByRole("textbox", { name: "感想" }).fill(reviewAfter);
  await page.getByRole("button", { name: "5点" }).click();
  const secondPhotoSource = await page
    .getByAltText("写真 2枚目")
    .getAttribute("src");
  // dnd-kit は、つかんだあとに写真の位置を測る。測り終わる前の矢印キーは無視されるので、
  // 読み上げ欄（画面には見えない。dnd-kit が今の状態を文で書きかえる）で、1 つずつ終わったのを確かめてから次のキーを押す
  const dragStatus = page.locator('[id^="DndLiveRegion-"]');
  await page.getByRole("button", { name: "2枚目の写真を動かす" }).focus();
  await page.keyboard.press("Space");
  // 位置を測り終えると、今いる場所（自分の位置）を知らせる
  await expect(dragStatus).toHaveText("2枚目の位置です。");
  await page.keyboard.press("ArrowLeft");
  await expect(dragStatus).toHaveText("1枚目の位置です。");
  await page.keyboard.press("Space");
  await expect(dragStatus).toHaveText("2枚目の写真を、1枚目に置きました。");
  // 置いたあと、先頭（1枚目）が、もとの 2 枚目の写真になっている
  await expect(page.getByAltText("写真 1枚目")).toHaveAttribute(
    "src",
    secondPhotoSource ?? "",
  );

  // ④ 保存すると、詳細画面へ戻り、変更が出る
  await page.getByRole("button", { name: "保存する" }).click();
  await expect(page).toHaveURL(`/records/${createdRecordId}`, {
    timeout: 20_000,
  });
  const expectedPhotos = [photosBefore[1], photosBefore[0]];

  async function expectEdited() {
    await expect(page.getByText(reviewAfter)).toBeVisible();
    await expect(page.getByText("評価 5（5段階）")).toBeAttached();
    expect(await photoPathsOnDetail(page)).toEqual(expectedPhotos);
  }
  await expectEdited();

  // ⑤ 読み直しても残っている（保存されている）
  await page.reload();
  await expectEdited();
});
