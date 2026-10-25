# config

バックエンドの開発で使う設定ファイルを置く場所です。

## 整形のしくみ（Spotless）

整形は Spotless（`build.gradle` の `spotless { … }`）で行い、Spotless の結果を正とします。
コミットの前に `./gradlew spotlessApply` で整形し、`./gradlew spotlessCheck` で確かめます。

| 対象 | 使うもの | 補足 |
|---|---|---|
| Java（`src/main/java`・`src/test/java`） | 使っていない import の削除 → import の並び替え（Google スタイル：static、空行 1 行、そのほか） → Eclipse の整形（下の `eclipse-java-google-style.xml`） | Cursor の保存時の整形と import の整理も、同じ XML と同じ import の順を使う。ただし Cursor の設定（`.vscode/settings.json`）は Git の管理外の各自のローカル設定なので、clone しただけでは保存時の整形はそろわない。import の行の後ろや import の間に書いたコメントは、import の並び替えで消える（import より前の行のコメントは残る） |
| mapper の XML（`src/main/resources/mapper`） | Prettier と `@prettier/plugin-xml`（`xmlWhitespaceSensitivity: preserve`） | タグ・属性・コメントの字下げはそろえるが、SQL の空白と改行は書いたまま残す（くずれた SQL の字下げを直すわけではない）。SQL の中の字下げは手でそろえる。Cursor の保存時の整形も同じ Prettier・同じ設定を使う（下の「mapper の XML の保存時の整形」） |

mapper の XML の整形には Node.js と npm が要ります（Spotless が npm で Prettier を `build/` の中に入れて動かします）。
このプロジェクトのパッケージ管理は pnpm ですが、Spotless の `prettier()` は npm を使う作りのため、ここだけ例外として npm を使っています（自分で npm のコマンドを打つことはありません）。
Spotless に渡す版は、`backend/package.json` の `devDependencies`（`prettier` と `@prettier/plugin-xml`）から `build.gradle` が読みこみます。
版を固定しているのはこの主要な 2 つだけで、その下で使われるパッケージ（間接的な依存）の版までは固定していません。
Spotless が npm で入れるものと、Cursor 用に pnpm で入れるもの（`backend/pnpm-lock.yaml`）は別なので、間接的な依存まで同じになるとは限りません。
入れ直したときに整形の結果が変われば `spotlessCheck` で気づけますが、依存の変化そのものや、前と同じ環境に戻せることまでは保証しません。
Spotless は `check` に `spotlessCheck` をつなぐので、Node.js のない環境では `./gradlew build` も失敗します。

## mapper の XML の保存時の整形（Cursor）

Cursor で mapper の XML を保存（Ctrl+S）したときも、Spotless と同じ Prettier・同じ設定で整形します。

| ファイル | 役割 |
|---|---|
| `backend/package.json` | Prettier と `@prettier/plugin-xml` の版（`^` なしで固定）。**版を書くのはここだけ**（`build.gradle` もここを読む） |
| `backend/pnpm-lock.yaml` | 上の 2 つと、その下で使われるパッケージの版（コミットする） |
| `backend/.prettierrc.json` | 整形の設定。**設定を書くのはここだけ**（Spotless も `configFile` で読む）。`parser` は書かない（拡張子から決まる。書くと JSON なども XML として扱われる） |
| `backend/.prettierignore` | 整形しないファイル。`config/eclipse-java-google-style.xml` を入れている（取得元からの変更を 1 か所だけにしておくため） |

**`pnpm install` が要るとき**

clone したあと・別の PC で使うとき、それに `backend/package.json` か `backend/pnpm-lock.yaml` が変わったのを pull したときに、`backend` で実行します。
pull しただけでは、`backend/node_modules` は古いままなので、Cursor の保存と Spotless で使う Prettier の版がずれることがあります。

```powershell
cd backend
pnpm install --frozen-lockfile
```

**Cursor の設定（各自のローカル設定）**

`.vscode/settings.json` は Git の管理外なので、clone しただけでは保存時の整形は有効になりません。次を足します（Prettier の拡張機能 `esbenp.prettier-vscode` が要ります）。

```json
"[xml]": {
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.formatOnSave": true
}
```

`[xml]` は mapper だけでなく、すべての XML に効きます。mapper 以外で整形したくない XML は、`backend/.prettierignore` に足します。
Prettier の拡張機能は、リポジトリのいちばん上に `.prettierignore` がないとき、保存したファイルのフォルダーから上へ順に `.prettierignore` を探すので、`backend` のファイルには `backend/.prettierignore` が使われます。
（拡張機能 12.4.0 の中のコードと、今のワークスペースの形（Cursor でリポジトリのいちばん上を開き、`prettier.ignorePath` は設定していない）で確かめた内容です。
リポジトリのいちばん上に `.prettierignore` を足したときは、`backend` と `frontend` の除外が今までどおり効くか、確かめ直します。）

**版や設定を変えるとき**

1. `backend` で `pnpm add -D --save-exact prettier@<版> @prettier/plugin-xml@<版>`（`package.json` と `pnpm-lock.yaml` が変わる）。設定を変えるときは `backend/.prettierrc.json` を直す
2. `./gradlew spotlessApply --rerun-tasks` と `./gradlew spotlessCheck --rerun-tasks` で確かめる（設定のファイルを変えても、Spotless が前の結果を使い回すことがあるため、`--rerun-tasks` を付ける）

**整形がおかしいとき**

`backend` で `pnpm install` をしていない、プラグインが見つからない、などのときは、拡張機能の中の Prettier で整形されたり、エラーになったりすることがあります。
Cursor の「出力」→「Prettier」のログで、使われた Prettier の場所（`backend\node_modules\prettier`）・版・設定のファイル（`backend\.prettierrc.json`）を確かめます。

**Prettier のコマンドで直接確かめるとき**

Windows では、Git の設定（`core.autocrlf`）で mapper の XML の改行が CRLF になっています。Prettier は初めの設定で改行を LF にそろえるので、そのまま `prettier --check` をすると、すべて「整形が必要」と出ます。
`pnpm exec prettier --check --end-of-line auto "src/main/resources/mapper/*.xml"` のように、`--end-of-line auto` を付けます。
（Spotless と Cursor の保存については、今の Windows と Cursor の環境で、保存の前と後でファイルの中身が変わらないことと、`spotlessCheck` が成功することを確かめています。どの環境でも同じとは限りません。）

## eclipse-java-google-style.xml

Java の整形のルール（Google スタイル、Eclipse の整形用。プロファイル名 `GoogleStyle`）です。
Spotless（`build.gradle` の `spotless { java { eclipse(...).configFile(...) } }`）と、エディタの保存時の整形で使います。
このファイル自体は Prettier で整形しません（`backend/.prettierignore` に入れている）。

| 項目 | 内容 |
|---|---|
| 取得元 | [google/styleguide](https://github.com/google/styleguide) の `eclipse-java-google-style.xml` |
| 取得したコミット | [`505ba68c74eb97e6966f60907ce893001bedc706`](https://github.com/google/styleguide/blob/505ba68c74eb97e6966f60907ce893001bedc706/eclipse-java-google-style.xml)（2018-01-30。google/styleguide から削除される前の最後の版） |
| 取得した URL | https://raw.githubusercontent.com/google/styleguide/505ba68c74eb97e6966f60907ce893001bedc706/eclipse-java-google-style.xml |
| ライセンス | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)（取得したコミットの README に「The style guides in this project are licensed under the CC-By 3.0 License」とある） |
| 作者 | Google |
| 変更 | 1 か所（下の「取得元からの変更」） |

### 取得元からの変更

| 行 | 設定 | 取得元 | このリポジトリ | 理由 |
|---|---|---|---|---|
| 193 | `org.eclipse.jdt.core.formatter.number_of_empty_lines_to_preserve`（続いた空行を何行まで残すか） | `3` | `1` | 続いた空行を 1 行にそろえるため。Google Java Style Guide の「4.6.1 Vertical whitespace」でも、続いた空行は「permitted, but never required (or encouraged)」とされている |

取得元のファイルの Git の blob の SHA-1 は `7bb6804eb39f988b43f6ac997d66c203272d4969` です。取得元と比べるときは、上の 1 か所だけがちがいます。

### リポジトリに置いている理由

以前はエディタの設定で、google/styleguide の `gh-pages` ブランチの URL を直接参照していました。
このファイルは 2026-10-14 のコミット [`355ad72`](https://github.com/google/styleguide/commit/355ad72e6b01efe6a29481c627068a381dc0af4b)（「java-google-style: drop old eclipse & intellij style files」）で削除され、その URL は見つからなくなりました。
元のファイルが変わったり消えたりしても整形のルールが変わらないように、版を決めてリポジトリに置いています。

google/styleguide は現在、Java の整形には [google-java-format](https://github.com/google/google-java-format) を使うようにすすめています。
このプロジェクトは、これまでの書き方を保つため、今はこの XML を使います。
