-- 商品名の大文字小文字の扱いの確認用（V4：同じ店の中では LOWER(商品名) が同じものは 1 つだけ）。
-- 店1には、大文字小文字以外の違い（小さいかな・半角カナ・丸数字・アクセント）で区別したい商品も入れる。
INSERT INTO shops (id, shop_name, area_id, shop_kind_id, is_closed)
VALUES (1, 'スタバ', 20, 9, false),
       (2, 'ドトール', 20, 9, false);

INSERT INTO items (id, shop_id, item_name, category_id, is_seasonal)
VALUES (1, 1, 'Latte', 10, true),
       (2, 1, 'きゃべつ', 1, false),
       (3, 1, 'ラーメン', 2, false),
       (4, 1, '1番', 1, false),
       (5, 1, 'Café', 10, false),
       (6, 1, 'Mocha', 10, false);
