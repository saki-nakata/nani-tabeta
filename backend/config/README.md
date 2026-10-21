# config

バックエンドの開発で使う設定ファイルを置く場所です。

## 整形のしくみ（Spotless）

整形は Spotless（`build.gradle` の `spotless { … }`）で行い、Spotless の結果を正とします。
コミットの前に `./gradlew spotlessApply` で整形し、`./gradlew spotlessCheck` で確かめます。

| 対象 | 使うもの | 補足 |
|---|---|---|
| Java（`src/main/java`・`src/test/java`） | 使っていない import の削除 → import の並び替え（Google スタイル：static、空行 1 行、そのほか） → Eclipse の整形（下の `eclipse-java-google-style.xml`） | Cursor の保存時の整形と import の整理も、同じ XML と同じ import の順を使う。ただし Cursor の設定（`.vscode/settings.json`）は Git の管理外の各自のローカル設定なので、clone しただけでは保存時の整形はそろわない。import の行の後ろや import の間に書いたコメントは、import の並び替えで消える（import より前の行のコメントは残る） |
| mapper の XML（`src/main/resources/mapper`） | Prettier と `@prettier/plugin-xml`（`xmlWhitespaceSensitivity: preserve`） | タグとコメントの字下げはそろえるが、SQL の空白と改行は書いたまま残す。SQL の中の字下げは手でそろえる。Cursor の保存時の整形は使わない |

mapper の XML の整形には Node.js と npm が要ります（Spotless が npm で Prettier を `build/` の中に入れて動かします）。
このプロジェクトのパッケージ管理は pnpm ですが、Spotless の `prettier()` は npm を使う作りのため、ここだけ例外として npm を使っています（自分で npm のコマンドを打つことはありません）。
版を固定しているのは `build.gradle` に書いた主要なパッケージ（`prettier` と `@prettier/plugin-xml`）だけで、その下で使われるパッケージ（間接的な依存）の版までは固定していません。
入れ直したときに整形の結果が変われば `spotlessCheck` で気づけますが、依存の変化そのものや、前と同じ環境に戻せることまでは保証しません。
Spotless は `check` に `spotlessCheck` をつなぐので、Node.js のない環境では `./gradlew build` も失敗します。

## eclipse-java-google-style.xml

Java の整形のルール（Google スタイル、Eclipse の整形用。プロファイル名 `GoogleStyle`）です。
Spotless（`build.gradle` の `spotless { java { eclipse(...).configFile(...) } }`）と、エディタの保存時の整形で使います。

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
