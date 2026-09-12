# 今日なに食べた？（Nani Tabeta）

いろんな味を楽しむための、食べたものの記録アプリ。
「これ前に食べたっけ？」に、その日の感想つきで答えます。

## 開発状況

設計・環境構築・DB構築が完了し、これからバックエンドの実装に入ります。

- [x] 要件定義
- [x] ER図・テーブル設計
- [x] 技術選定
- [x] 環境構築（Spring Boot / MySQL / Next.js）
- [x] DB設計・マイグレーション
- [ ] バックエンド実装
- [ ] フロントエンド実装
- [ ] デプロイ

## 技術構成

### 構築済み

| 領域 | 技術 |
|---|---|
| フロントエンド | Next.js 16.3.4 (App Router) / React 19.2.8 / TypeScript 5.9 / Tailwind CSS v4 |
| バックエンド | Java 25 / Spring Boot 4.0.8 / MyBatis / Flyway |
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

**テーブルの作成は不要です。** 起動時に Flyway が
`backend/src/main/resources/db/migration/` の SQL を自動で適用し、
テーブルとマスタデータ（地域・エリア・分類・業態）を作ります。

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

## データベース

スキーマの変更は **Flyway** で管理します。SQL はこの場所に置きます。

```
backend/src/main/resources/db/migration/
├── V1__init.sql          テーブル定義（12テーブル）
└── V2__master_data.sql   マスタの初期データ
```

| 決まりごと | 内容 |
|---|---|
| ファイル名 | `V<番号>__<説明>.sql`（**アンダースコアは2つ**）|
| 適用 | アプリの起動時に、未適用のものが番号順に実行される |
| 記録 | `flyway_schema_history` テーブルに適用状況が残る |

### 既存ファイルを編集してよい条件

Flyway は適用済みファイルの内容を照合値で検証するため、変更するとアプリが起動しなくなります。

```
Migration checksum mismatch for migration version 1
```

そのため、**既存ファイルの編集は次の条件を両方満たす場合に限ります。**

- `main` にマージしていない
- 適用済みの開発用DBを `docker compose down -v` で破棄できる

この条件を満たす間は、`V1` を直接編集して作り直すほうが、履歴が読みやすくなります。

```powershell
docker compose down -v   # データと適用履歴をまとめて削除する
docker compose up -d
cd backend
.\gradlew bootRun        # V1 から再適用される
```

**`main` にマージした後は、既存ファイルを変更せず、`V3__xxx.sql` のように追加します。**
他の環境（本番を含む）に適用済みのファイルは、もう書き換えられないためです。

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
