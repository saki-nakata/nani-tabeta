# backend

「今日なに食べた？」のバックエンド。
Java 25 / Spring Boot 4.0.8 / MyBatis。

セットアップ手順は [ルートの README](../README.md) を参照してください。
実行には MySQL が起動している必要があります。

## コマンド

Gradle Wrapper が同梱されているため、Gradle のインストールは不要です。

| 目的 | コマンド |
|---|---|
| テスト | `.\gradlew test` |
| 起動 | `.\gradlew bootRun` |
| ビルド | `.\gradlew build` |

起動すると http://localhost:8080 で待ち受けます。

## 設定

`src/main/resources/application.yaml` で DB 接続を設定しています。
パスワードはファイルに書かず、リポジトリルートの `.env` から読み込みます。

```yaml
spring:
  config:
    import:
      - optional:file:.env[.properties]
      - optional:file:../.env[.properties]
  datasource:
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
```

`optional:` を付けているため、`.env` が存在せず環境変数が直接渡される本番環境でも
同じファイルがそのまま使えます。

## テスト

| クラス | 内容 |
|---|---|
| `BackendApplicationTests` | Spring のコンテキストが起動するか |
| `DbConnectionTest` | MySQL に実際に接続できるか |
