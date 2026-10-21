-- 候補で完全一致を先に並べることの確認用（店）。
-- エリア20〜22は、部分一致する店が名前順で 10 件あり、完全一致の店が 11 件目以降になるようにしている
-- （完全一致を先に並べないと、上限 10 件で漏れることを再現するため）。

-- エリア20：完全一致「Latte」と、名前順でその前に並ぶ「A01 Latte」〜「A10 Latte」、大文字小文字が違う「latte」
INSERT INTO shops (shop_name, area_id, shop_kind_id, is_closed)
VALUES ('A01 Latte', 20, 9, false), ('A02 Latte', 20, 9, false), ('A03 Latte', 20, 9, false),
       ('A04 Latte', 20, 9, false), ('A05 Latte', 20, 9, false), ('A06 Latte', 20, 9, false),
       ('A07 Latte', 20, 9, false), ('A08 Latte', 20, 9, false), ('A09 Latte', 20, 9, false),
       ('A10 Latte', 20, 9, false), ('Latte', 20, 9, false), ('latte', 20, 9, false);

-- エリア21：% _ バックスラッシュを含む完全一致「100%_\OFF」と、名前順でその前に並ぶ「0 100%_\OFF 01」〜「10」
INSERT INTO shops (shop_name, area_id, shop_kind_id, is_closed)
VALUES ('0 100%_\\OFF 01', 21, 1, false), ('0 100%_\\OFF 02', 21, 1, false),
       ('0 100%_\\OFF 03', 21, 1, false), ('0 100%_\\OFF 04', 21, 1, false),
       ('0 100%_\\OFF 05', 21, 1, false), ('0 100%_\\OFF 06', 21, 1, false),
       ('0 100%_\\OFF 07', 21, 1, false), ('0 100%_\\OFF 08', 21, 1, false),
       ('0 100%_\\OFF 09', 21, 1, false), ('0 100%_\\OFF 10', 21, 1, false),
       ('100%_\\OFF', 21, 1, false);

-- エリア22：カタカナの「ラテ」と、名前順でその前に並ぶ「A01 ラテ」〜「A10 ラテ」
INSERT INTO shops (shop_name, area_id, shop_kind_id, is_closed)
VALUES ('A01 ラテ', 22, 9, false), ('A02 ラテ', 22, 9, false), ('A03 ラテ', 22, 9, false),
       ('A04 ラテ', 22, 9, false), ('A05 ラテ', 22, 9, false), ('A06 ラテ', 22, 9, false),
       ('A07 ラテ', 22, 9, false), ('A08 ラテ', 22, 9, false), ('A09 ラテ', 22, 9, false),
       ('A10 ラテ', 22, 9, false), ('ラテ', 22, 9, false);

-- エリア23：濁点だけが違う「からあけ」と「からあげ」（名前順では「からあけ」が先）
INSERT INTO shops (shop_name, area_id, shop_kind_id, is_closed)
VALUES ('からあけ', 23, 1, false), ('からあげ', 23, 1, false);

-- エリア24：完全一致「Latte」は閉店している（候補の条件を満たさないので、先頭にも出ない）
INSERT INTO shops (shop_name, area_id, shop_kind_id, is_closed)
VALUES ('Latte', 24, 9, true), ('A Latte', 24, 9, false);

-- エリア25：1文字の「ガ」（U+30AC）と、部分一致の候補になる「ガスト」。
-- 「カ」＋結合濁点（U+30AB U+3099）で探すと、= では「ガ」と一致するが、LIKE は1文字ずつ比べるので一致しない
INSERT INTO shops (shop_name, area_id, shop_kind_id, is_closed)
VALUES ('ガ', 25, 1, false), ('ガスト', 25, 2, false);

-- エリア26：閉店した「ガ」（結合濁点で完全一致しても、閉店の条件で候補に出ない）
INSERT INTO shops (shop_name, area_id, shop_kind_id, is_closed)
VALUES ('ガ', 26, 1, true);
