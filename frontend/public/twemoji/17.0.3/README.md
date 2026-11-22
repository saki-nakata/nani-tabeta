# Twemoji の画像（v17.0.3）

分類・業態の絵文字を、どの端末でも同じ絵で表示するための画像です。アプリの `public/` から配信します（外部の CDN は使いません）。

| 項目 | 内容 |
|---|---|
| 取得元 | https://github.com/jdecked/twemoji のタグ `v17.0.3`（`assets/svg/` の SVG すべて） |
| 取得したファイル | `https://github.com/jdecked/twemoji/archive/refs/tags/v17.0.3.tar.gz`（SHA-256：`a0855654b633045ae2337537e77f1bb4361162f7fcd910e613eaab1d6d9c5fca`） |
| 取得日 | 2026-11-21 |
| ファイル | SVG 4,009 個（合計 約 9.7MB）。名前は絵文字のコードポイント（例：☕ → `2615.svg`） |
| ライセンス | 画像は **CC-BY 4.0**（`LICENSE-GRAPHICS`、https://creativecommons.org/licenses/by/4.0/）。Twemoji の README の「Attribution Requirements」では、README か、サイトのフッター・「About」での表記を帰属表示として認めている。このアプリは、画面のフッターと README の両方に出す |
| 著作権者 | Twemoji の `LICENSE` の表記：Copyright (c) 2022–present Jason Sofonia & Justine De Caires / Copyright (c) 2014–2021 Twitter |

画像は取得したままで、手を加えていません。

## 版を上げるとき

1. 新しいタグの `assets/svg/` と `LICENSE-GRAPHICS` を、新しい版のフォルダ（例：`public/twemoji/17.1.0/`）に置く
2. コードの版の番号（画像の場所）と、`@twemoji/api` の版をそろえて変える
3. 古い版のフォルダは、使われていないことを確かめてから外す
