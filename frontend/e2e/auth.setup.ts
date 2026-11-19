import { createServerClient } from "@supabase/ssr";
import { expect, test as setup } from "@playwright/test";
import { AUTH_FILE, requireEnv } from "./support/env";

// テスト用アカウント（テスト1）でログインし、その状態を保存する
// 画面のログインは Google だけなので、メールアドレスとパスワードで Supabase に直接ログインし、
// アプリと同じ形の Cookie をブラウザに入れる（Cookie の名前と中身は @supabase/ssr に作らせる）
setup("テスト用アカウントでログインする", async ({ page }) => {
  const cookies: { name: string; value: string }[] = [];
  const supabase = createServerClient(
    requireEnv("NEXT_PUBLIC_SUPABASE_URL"),
    requireEnv("NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY"),
    {
      cookies: {
        getAll: () => [],
        // ログインできたら、@supabase/ssr がここに「入れるべき Cookie」を渡してくる
        setAll: (cookiesToSet) => {
          for (const { name, value } of cookiesToSet) {
            cookies.push({ name, value });
          }
        },
      },
    },
  );

  const { error } = await supabase.auth.signInWithPassword({
    email: requireEnv("E2E_USER_EMAIL"),
    password: requireEnv("E2E_USER_PASSWORD"),
  });
  if (error) {
    throw new Error(
      `テスト用アカウントでログインできませんでした：${error.message}`,
    );
  }
  expect(cookies.length).toBeGreaterThan(0);

  await page.context().addCookies(
    cookies.map((cookie) => ({
      ...cookie,
      domain: "localhost",
      path: "/",
      sameSite: "Lax" as const,
    })),
  );

  // ログインできていれば、記録の一覧（ログインした人だけが見られる画面）が出る
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "これまでの記録" }),
  ).toBeVisible();

  await page.context().storageState({ path: AUTH_FILE });
});
