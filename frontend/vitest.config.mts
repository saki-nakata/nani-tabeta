import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";
import { configDefaults, defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    environment: "jsdom",
    // e2e フォルダは Playwright の E2E テスト（pnpm exec playwright test で動かす）なので、Vitest では動かさない
    // delete フォルダは、消す前に移しておくだけの場所（Git 管理外）なので、中のファイルは動かさない
    exclude: [...configDefaults.exclude, "e2e/**", "delete/**"],
  },
});
