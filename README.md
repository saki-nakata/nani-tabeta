# 今日なに食べた？（Nani Tabeta）

いろんな味を楽しむための、食べたものの記録アプリ。
「これ前に食べたっけ？」に、その日の感想つきで答えます。

## 開発状況

設計と環境構築が完了し、これから実装に入ります。

- [x] 要件定義
- [x] ER図・テーブル設計
- [x] 技術選定
- [x] 環境構築（Spring Boot / MySQL / Next.js）
- [ ] バックエンド実装
- [ ] フロントエンド実装
- [ ] デプロイ

## 技術構成

### 構築済み

| 領域 | 技術 |
|---|---|
| フロントエンド | Next.js 16.3.4 (App Router) / React 19.2.8 / TypeScript 5.9 / Tailwind CSS v4 |
| バックエンド | Java 25 / Spring Boot 4.0.8 / MyBatis |
| データベース | MySQL 8.4（Docker Compose）|

### 今後実装

| 領域 | 技術 |
|---|---|
| 認証 | Supabase Auth（Google OAuth）＋ Spring Security による JWT 検証 |
| ファイル保存 | Supabase Storage |
| UIコンポーネント | shadcn/ui |
| 外部API | Google Places API（店名の入力補助）|
| デプロイ | Vercel（フロントエンド）/ Railway（バックエンド＋MySQL）|

## 必要な環境

| ソフトウェア | 要件 | 動作確認したバージョン |
|---|---|---|
| JDK | 25 | Eclipse Temurin 25.0.4.1 |
| Docker Desktop | - | 28.2.2 |
| Node.js | 20.9 以上 | 24.11.0 |
| pnpm | 11 以上 | 11.9.0 |

Gradle は Wrapper が同梱されているため、個別のインストールは不要です。

## セットアップ

PowerShell（Windows）での手順です。

### 1. リポジトリを取得する

```powershell
git clone https://github.com/saki-nakata/nani-tabeta.git
cd nani-tabeta
```

### 2. 環境変数を用意する

```powershell
Copy-Item .env.sample .env
```

`.env` を開き、パスワードを設定します。

```
MYSQL_ROOT_PASSWORD=（任意の値）
MYSQL_PASSWORD=（root とは別の値）
```

`.env` は Git 管理外です。コミットしないでください。

### 3. MySQL を起動する

```powershell
docker compose up -d
docker compose ps
```

`STATUS` が `Up (healthy)` になるまで30秒ほどかかります。

### 4. バックエンドを起動する

```powershell
cd backend
.\gradlew test        # DB に接続できるか確認する
.\gradlew bootRun     # http://localhost:8080
```

### 5. フロントエンドを起動する

```powershell
cd frontend
pnpm install
pnpm dev              # http://localhost:3000
```

## よく使うコマンド

| 目的 | コマンド | 実行場所 |
|---|---|---|
| MySQL の起動 | `docker compose up -d` | ルート |
| MySQL の停止 | `docker compose down` | ルート |
| MySQL に接続 | `docker compose exec db mysql -u nani_tabeta -p nani_tabeta` | ルート |
| バックエンドのテスト | `.\gradlew test` | `backend` |
| バックエンドの起動 | `.\gradlew bootRun` | `backend` |
| フロントエンドの起動 | `pnpm dev` | `frontend` |
| 静的検査 | `pnpm lint` | `frontend` |
| 本番ビルドの確認 | `pnpm build` | `frontend` |

## 注意

**`docker compose down -v` はデータベースの中身をすべて削除します。**
`-v` は named volume（データの保管庫）を消すオプションです。
`-v` を付けない `down` であればデータは残ります。

ポート 3306 / 8080 / 3000 を使用します。
ローカルに MySQL をインストールしている場合は、先にサービスを停止してください。

```powershell
Stop-Service MySQL80
```

## ドキュメント

- [要件定義](docs/要件定義.md)
- [ER図・テーブル設計](docs/テーブル設計.md)
- [技術選定の理由](docs/技術選定.md)
