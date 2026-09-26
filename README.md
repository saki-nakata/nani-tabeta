# 今日なに食べた？（Nani Tabeta）

いろんな味を楽しむための、食べたものの記録アプリ。
「これ前に食べたっけ？」に、その日の感想つきで答えます。

## 開発状況

バックエンドを実装中です。マスタ参照・店・商品の API と、認証（JWT の検証・ユーザー登録）まで完成しています。

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
| 認証（サーバー側） | Spring Security による Supabase Auth の JWT 検証 |

### 今後実装

| 領域 | 技術 |
|---|---|
| 認証（ログイン画面） | Supabase Auth（Google OAuth） |
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

`.env` を開き、次の値を設定します。

```
MYSQL_ROOT_PASSWORD=（任意の値）
MYSQL_PASSWORD=（root とは別の値）
SUPABASE_URL=https://<project-ref>.supabase.co
```

`SUPABASE_URL` は、Supabase のダッシュボードの **Project Settings → API** にある **Project URL** です。
バックエンドはこの URL から、JWT の発行者（`/auth/v1`）と、署名を確かめる公開鍵（`/auth/v1/.well-known/jwks.json`）の場所を決めます。

- **末尾に `/` を付けないでください。** 付けると `…supabase.co//auth/v1` になり、すべての JWT が検証に失敗します
- `/rest/v1/` など、後ろのパスも付けません

`.env` は Git 管理外です。コミットしないでください。

### 3. MySQL を起動する

```powershell
docker compose up -d
docker compose ps
```

`STATUS` が `Up (healthy)` になるまで30秒ほどかかります。

初めて起動したときは、開発用の `nani_tabeta` に加えて、テスト用の `nani_tabeta_test` も自動で作られます。
以前からこのリポジトリで MySQL を使っていた場合は、[テスト用データベース](#テスト用データベース) の手順で追加してください。

### 4. バックエンドを起動する

```powershell
cd backend
.\gradlew test        # テスト用DB（nani_tabeta_test）で実行する
.\gradlew bootRun     # http://localhost:8080
```

**テーブルの作成は不要です。** 起動時に Flyway が
`backend/src/main/resources/db/migration/` の SQL を自動で適用し、
テーブルとマスタデータ（地域・エリア・分類・業態）を作ります。

API は Swagger UI（http://localhost:8080/swagger-ui/index.html）で確認できます。
Swagger UI 以外の API は Supabase Auth の JWT が必要です。右上の **Authorize** に JWT（`access_token`）を入力すると、
Swagger UI から送るリクエストに `Authorization: Bearer …` が付きます。
JWT の `sub` のユーザーがまだ登録されていない場合は、`POST /api/users` 以外は 403 になります。

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
| MySQL に接続 | `docker compose exec db mysql --default-character-set=utf8mb4 -u nani_tabeta -p nani_tabeta` | ルート |
| テスト用DBに接続 | `docker compose exec db mysql --default-character-set=utf8mb4 -u nani_tabeta -p nani_tabeta_test` | ルート |
| バックエンドのテスト | `.\gradlew test` | `backend` |
| バックエンドの起動 | `.\gradlew bootRun` | `backend` |
| フロントエンドの起動 | `pnpm dev` | `frontend` |
| 静的検査 | `pnpm lint` | `frontend` |
| 本番ビルドの確認 | `pnpm build` | `frontend` |

### MySQL に接続するときの注意（Windows）

**`--default-character-set=utf8mb4` を付ける。** 付けないと接続の文字コードが `latin1` になり、
`COLLATION 'utf8mb4_0900_ai_ci' is not valid for CHARACTER SET 'latin1'` というエラーになります。

**引用符を入れ子にしない。** `sh -c '...'` の中に `-e "..."` を書くと、PowerShell から渡す
途中で内側の `"` が失われ、SQL が空白で分割されて構文エラーになります。

```powershell
# NG: sh -c の中に -e "..." を入れ子にすると壊れる
docker compose exec db sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" nani_tabeta -e "INSERT INTO ...;"'

# OK: docker に直接渡す（SQL は1つの引数として届く）
docker compose exec db mysql --default-character-set=utf8mb4 -u nani_tabeta -p nani_tabeta -e "SELECT * FROM shops;"

# OK: mysql に入ってから SQL を実行する（複数の SQL を続けて書くときはこちら）
docker compose exec db mysql --default-character-set=utf8mb4 -u nani_tabeta -p nani_tabeta
```

SQL の中の文字列はシングルクォート（`'パン工房'`）で囲みます。

**日本語を貼り付ける場合は、先に `chcp 65001` を実行する。** コンソールの文字コードが
UTF-8 でないと、日本語が `?` や空文字になったまま登録されます。

## データベース

スキーマの変更は **Flyway** で管理します。SQL はこの場所に置きます。

```
backend/src/main/resources/db/migration/
├── V1__init.sql                              テーブル定義（12テーブル）
├── V2__master_data.sql                       マスタの初期データ
└── V3__use_japanese_collation_for_names.sql  店名・商品名の照合順序
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

### テスト用データベース

テストは開発用の `nani_tabeta` ではなく、**テスト専用の `nani_tabeta_test`** に接続します。
開発中に登録したデータでテストの結果が変わったり、テストが開発用のデータを書き換えたりしないようにするためです。

| 項目 | 内容 |
|---|---|
| 接続先の切り替え | `backend/src/test/resources/application-test.yaml` で接続先の URL だけを上書きする |
| 切り替えの有効化 | `backend/build.gradle` の `test` タスクで `spring.profiles.active=test` を指定する（テストクラスごとの指定は不要）|
| テーブルとマスタデータ | テストの実行時に Flyway が `V1`・`V2` を適用する（テスト専用の `schema.sql` や `data.sql` は置かない）|
| DB の作成 | `docker/mysql/init/01_create_test_database.sql` |

#### 新しく clone した場合

追加の作業はありません。`docker compose up -d` を初めて実行したときに、
`docker/mysql/init/` の SQL が自動で実行され、`nani_tabeta_test` の作成と権限の付与が行われます。

#### すでに MySQL のボリュームがある場合

`docker/mysql/init/` の SQL は、**データの保存先（ボリューム）が空のときの初回起動時にしか実行されません。**
以前から MySQL を使っていた環境では自動で作られないため、次のコマンドで一度だけ作成します。

```powershell
docker compose up -d
Get-Content docker/mysql/init/01_create_test_database.sql | docker compose exec -T db sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD"'
```

`Using a password on the command line interface can be insecure.` という警告だけが表示されれば成功です。
SQL は `IF NOT EXISTS` 付きなので、何度実行しても問題ありません。

> `docker compose down -v` でボリュームを作り直す方法でも作られますが、開発用のデータもすべて消えます。

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
