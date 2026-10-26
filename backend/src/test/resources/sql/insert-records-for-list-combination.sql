-- insert-records-for-list.sql に足して使う。分類と業態の組み合わせの確認用。
-- 分類10（ドリンク）だが業態1（コンビニ）の商品4と、業態9（カフェ）だが分類1（ごはんもの）の商品5を足す。
-- 分類10・業態9の両方に当てはまるのは、もとの記録4・2だけ
INSERT INTO items (id, shop_id, item_name, category_id, is_seasonal)
VALUES (4, 1, 'ホットコーヒー', 10, false),
       (5, 2, 'サンドイッチ', 1, false);

INSERT INTO records (id, user_id, item_id, eaten_on, rating)
VALUES (7, '20b622c8-dd31-46e8-8f1a-64009c7febe5', 4, '2026-10-22', 3),
       (8, '20b622c8-dd31-46e8-8f1a-64009c7febe5', 5, '2026-10-21', 3);
