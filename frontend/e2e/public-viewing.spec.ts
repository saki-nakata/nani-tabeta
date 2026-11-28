import { expect, test, type Browser, type Page } from "@playwright/test";

// 記録の詳細を、ログインしていない人も見られることを、本物のブラウザ・バックエンド・DB で確かめる
// 見ている人ごとの細かい出し分け（未登録の人・ゲストとして読み直した人など）は、Vitest（records/[id]/page.test.tsx）で確かめている

// テストで作った記録（最後に消すため）
let createdRecordId: string | null = null;

// ログインした状態（テスト用アカウント）で、記録を 1 件作り、記録の ID を返す
// photos に写真のファイルを渡すと、写真付きの記録にする（省略すると写真なし）
async function createRecord(
  page: Page,
  review: string,
  photos: string[] = [],
): Promise<string> {
  await page.goto("/records/new");
  if (photos.length > 0) {
    await page.locator('input[type="file"]').setInputFiles(photos);
  }
  await page.getByLabel("地方").selectOption("中部");
  await page.getByLabel("都道府県・国").selectOption("長野県");
  await page.getByRole("textbox", { name: "店名" }).fill("スタバ");
  await page.getByRole("button", { name: "スタバ", exact: true }).click();
  await page.getByRole("textbox", { name: "商品名" }).fill("ラテ");
  await page.getByRole("button", { name: "ラテ", exact: true }).click();
  await page.getByRole("button", { name: "3点" }).click();
  await page.getByRole("textbox", { name: "感想" }).fill(review);
  await page.getByRole("button", { name: "記録する" }).click();

  await expect(page).toHaveURL(/\/records\/\d+$/, { timeout: 20_000 });
  const id = page.url().match(/(\d+)$/)?.[1];
  if (!id) {
    throw new Error("作った記録の ID が URL から読めませんでした。");
  }
  return id;
}

// ログインしていない人（Cookie なし）のページを開く
async function openGuestPage(browser: Browser) {
  const context = await browser.newContext({
    storageState: { cookies: [], origins: [] },
  });
  return { context, page: await context.newPage() };
}

test.afterEach(async ({ page }) => {
  if (createdRecordId === null) {
    return;
  }
  // 作った記録を、詳細画面の「削除」で消す（テスト用アカウントでログインした状態）
  await page.goto(`/records/${createdRecordId}`);
  await page.getByRole("button", { name: "削除", exact: true }).click();
  await page.getByRole("button", { name: "削除する" }).click();
  await expect(page).toHaveURL("/");
  createdRecordId = null;
});

test("ログインしていない人も記録の詳細とニックネームを見られ、編集・削除は出ず、本人には自分の記録として出ること", async ({
  browser,
  page,
}) => {
  // テストのたびに違う感想にして、前のテストの記録とまちがえないようにする
  const review = `E2E 公開 ${Date.now()}`;
  // 写真付きにして、ログインしていない人にも写真が出ることまで確かめる（写真は Storage の公開バケットから出す）
  createdRecordId = await createRecord(page, review, [
    "e2e/fixtures/photo-a.jpg",
  ]);
  const detailPath = `/records/${createdRecordId}`;

  // ① ログインしていない人が詳細を開く
  const guest = await openGuestPage(browser);
  try {
    const response = await guest.page.goto(detailPath);
    // /login へ移されず、詳細が出ること
    await expect(guest.page).toHaveURL(detailPath);
    await expect(guest.page.getByText(review)).toBeVisible();
    // 写真も、ログインしていない人に出る（読み終わり、中身がある＝壊れた画像ではない）
    const photo = guest.page.getByAltText("ラテの写真");
    await expect(photo).toBeVisible();
    await expect
      .poll(() =>
        photo.evaluate(
          (element: HTMLImageElement) =>
            element.complete && element.naturalWidth > 0,
        ),
      )
      .toBe(true);
    // だれの記録かは、ニックネームで出る（「自分の記録」の札は出ない）
    await expect(guest.page.getByText(/^記録した人：.+/)).toBeVisible();
    await expect(guest.page.getByText("自分の記録")).toHaveCount(0);
    // 編集・削除は出ない
    await expect(guest.page.getByRole("link", { name: "編集" })).toHaveCount(0);
    await expect(
      guest.page.getByRole("button", { name: "削除", exact: true }),
    ).toHaveCount(0);
    // ヘッダーは、ゲスト向け（ログイン・登録）
    await expect(
      guest.page.getByRole("link", { name: "ログイン・登録" }),
    ).toBeVisible();
    await expect(
      guest.page.getByRole("link", { name: "＋ 記録する" }),
    ).toHaveCount(0);
    // 画面の返事が、途中のサーバーやブラウザに保存されない（ほかの人に、別の人向けの画面が返らない）
    expect(response?.headers()["cache-control"]).toContain("no-store");
  } finally {
    await guest.context.close();
  }

  // ② 本人（テスト用アカウント）が同じ詳細を開く
  await page.goto(detailPath);
  await expect(page.getByText(review)).toBeVisible();
  await expect(page.getByText("自分の記録")).toBeVisible();
  await expect(page.getByText(/^記録した人：/)).toHaveCount(0);
  await expect(page.getByRole("link", { name: "編集" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "削除", exact: true }),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: "＋ 記録する" })).toBeVisible();
});

test("ログインしていない人が、自分の記録の一覧・記録の登録・記録の編集を開くと、/login へ移ること", async ({
  browser,
  page,
}) => {
  const review = `E2E 公開しない画面 ${Date.now()}`;
  createdRecordId = await createRecord(page, review);

  const guest = await openGuestPage(browser);
  try {
    for (const path of [
      "/",
      "/records/new",
      `/records/${createdRecordId}/edit`,
    ]) {
      await guest.page.goto(path);
      await expect(guest.page).toHaveURL("/login");
    }
  } finally {
    await guest.context.close();
  }
});
