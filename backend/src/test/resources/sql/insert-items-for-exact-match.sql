-- 候補で完全一致を先に並べることの確認用。
-- 店1〜3は、部分一致する商品が名前順で 10 件あり、完全一致の商品が 11 件目以降になるようにしている
-- （完全一致を先に並べないと、上限 10 件で漏れることを再現するため）。
INSERT INTO shops (id, shop_name, area_id, shop_kind_id, is_closed)
VALUES (1, 'スタバ', 20, 1, false),
       (2, 'セール店', 20, 1, false),
       (3, 'カフェ', 20, 1, false),
       (4, 'セブンイレブン', 20, 1, false),
       (5, 'ローソン', 20, 1, false);

INSERT INTO users (id, nickname)
VALUES ('20b622c8-dd31-46e8-8f1a-64009c7febe5', 'Saki');

-- 店1：完全一致「Latte」と、名前順でその前に並ぶ「A01 Latte」〜「A10 Latte」
-- （大文字小文字だけが違う「latte」は、同じ店には UNIQUE で入れられない。V4）
INSERT INTO items (shop_id, item_name, category_id, is_seasonal)
VALUES (1, 'A01 Latte', 10, false), (1, 'A02 Latte', 10, false), (1, 'A03 Latte', 10, false),
       (1, 'A04 Latte', 10, false), (1, 'A05 Latte', 10, false), (1, 'A06 Latte', 10, false),
       (1, 'A07 Latte', 10, false), (1, 'A08 Latte', 10, false), (1, 'A09 Latte', 10, false),
       (1, 'A10 Latte', 10, false), (1, 'Latte', 10, false);

-- 店2：% _ バックスラッシュを含む完全一致「100%_\OFF」と、名前順でその前に並ぶ「0 100%_\OFF 01」〜「10」
INSERT INTO items (shop_id, item_name, category_id, is_seasonal)
VALUES (2, '0 100%_\\OFF 01', 11, false), (2, '0 100%_\\OFF 02', 11, false),
       (2, '0 100%_\\OFF 03', 11, false), (2, '0 100%_\\OFF 04', 11, false),
       (2, '0 100%_\\OFF 05', 11, false), (2, '0 100%_\\OFF 06', 11, false),
       (2, '0 100%_\\OFF 07', 11, false), (2, '0 100%_\\OFF 08', 11, false),
       (2, '0 100%_\\OFF 09', 11, false), (2, '0 100%_\\OFF 10', 11, false),
       (2, '100%_\\OFF', 11, false);

-- 店3：カタカナの「ラテ」と、名前順でその前に並ぶ「A01 ラテ」〜「A10 ラテ」
INSERT INTO items (shop_id, item_name, category_id, is_seasonal)
VALUES (3, 'A01 ラテ', 10, false), (3, 'A02 ラテ', 10, false), (3, 'A03 ラテ', 10, false),
       (3, 'A04 ラテ', 10, false), (3, 'A05 ラテ', 10, false), (3, 'A06 ラテ', 10, false),
       (3, 'A07 ラテ', 10, false), (3, 'A08 ラテ', 10, false), (3, 'A09 ラテ', 10, false),
       (3, 'A10 ラテ', 10, false), (3, 'ラテ', 10, false);

-- 店4：濁点だけが違う「からあけ」と「からあげ」（名前順では「からあけ」が先）
INSERT INTO items (shop_id, item_name, category_id, is_seasonal)
VALUES (4, 'からあけ', 4, false), (4, 'からあげ', 4, false);

-- 店5：1文字の「ガ」（U+30AC）と、部分一致の候補になる「ガム」。
-- 「カ」＋結合濁点（U+30AB U+3099）で探すと、= では「ガ」と一致するが、LIKE は1文字ずつ比べるので一致しない
INSERT INTO items (shop_id, item_name, category_id, is_seasonal)
VALUES (5, 'ガ', 9, false), (5, 'ガム', 9, false);

-- 候補は記録が 1 件以上ある商品だけを返すため、すべての商品に記録を付ける
INSERT INTO records (user_id, item_id, eaten_on, rating)
SELECT '20b622c8-dd31-46e8-8f1a-64009c7febe5', id, '2026-10-21', 3 FROM items;
