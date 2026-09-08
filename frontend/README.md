# frontend

「今日なに食べた？」のフロントエンド。
Next.js 16 (App Router) / TypeScript / Tailwind CSS v4。

セットアップ手順は [ルートの README](../README.md) を参照してください。

## コマンド

パッケージマネージャは **pnpm** です（`package.json` の `packageManager` で固定）。

| 目的 | コマンド |
|---|---|
| 依存関係のインストール | `pnpm install` |
| 開発サーバー | `pnpm dev` |
| 静的検査 | `pnpm lint` |
| 本番ビルド | `pnpm build` |

## ディレクトリ

```
src/app/              App Router のルート
  layout.tsx          全ページ共通のレイアウト
  page.tsx            トップページ
  globals.css         Tailwind の読み込みとテーマ定義
public/               画像などの静的ファイル
```
