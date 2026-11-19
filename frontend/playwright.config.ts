import { existsSync } from "node:fs";
import { defineConfig, devices } from "@playwright/test";
import { AUTH_FILE } from "./e2e/support/env";

// .env.local の値（Supabase の URL・キー、E2E 用アカウント）を読みこむ。Playwright は自動では読まないため
if (existsSync(".env.local")) {
  process.loadEnvFile(".env.local");
}

/**
 * E2E テストの設定（https://playwright.dev/docs/test-configuration）
 * 動かす前に、DB（docker compose）とバックエンド（Spring Boot）を起動しておく。フロントエンドは、
 * 起動していなければ Playwright が `pnpm dev` で起動する
 */
export default defineConfig({
  testDir: "./e2e",
  // 記録を作って消すテストがあるので、同時には動かさない（同じアカウントのデータを触るため）
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: "html",
  use: {
    baseURL: "http://localhost:3000",
    trace: "on-first-retry",
  },
  projects: [
    // ① テスト用アカウントでログインし、その状態を AUTH_FILE に保存する（ほかのテストより先に 1 回だけ動く）
    { name: "setup", testMatch: /.*\.setup\.ts/ },
    // ② 保存したログインの状態を使って、各機能のテストを動かす
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"], storageState: AUTH_FILE },
      dependencies: ["setup"],
    },
  ],
  webServer: {
    command: "pnpm dev",
    url: "http://localhost:3000",
    // もう起動していれば、それを使う（開発中は起動したままのことが多いため）
    reuseExistingServer: true,
  },
});
