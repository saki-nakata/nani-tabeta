-- 記録の一覧の確認用。
-- Saki の記録は、並び（食べた日の新しい順、同じ日は id の大きい順）で 4・3・5・2・1 になる。
-- 2025-12-31 と 2026-01-01 は、年の範囲の境目を確かめるため。
INSERT INTO users (id, nickname)
VALUES ('20b622c8-dd31-46e8-8f1a-64009c7febe5', 'Saki'),
       ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'ほかの人');

-- 店1はコンビニ（業態1）、店2はカフェ（業態9）
INSERT INTO shops (id, shop_name, area_id, shop_kind_id, is_closed)
VALUES (1, 'セブンイレブン', 20, 1, false),
       (2, 'スタバ', 20, 9, false);

-- 商品3だけ、分類10（ドリンク）
INSERT INTO items (id, shop_id, item_name, category_id, is_seasonal)
VALUES (1, 1, 'からあげ棒', 4, false),
       (2, 1, 'おにぎり', 1, false),
       (3, 2, 'Latte', 10, true);

INSERT INTO records (id, user_id, item_id, eaten_on, rating)
VALUES (1, '20b622c8-dd31-46e8-8f1a-64009c7febe5', 1, '2025-12-31', 3),
       (2, '20b622c8-dd31-46e8-8f1a-64009c7febe5', 3, '2026-01-01', 4),
       (3, '20b622c8-dd31-46e8-8f1a-64009c7febe5', 2, '2026-10-24', 5),
       (4, '20b622c8-dd31-46e8-8f1a-64009c7febe5', 3, '2026-10-24', 4),
       (5, '20b622c8-dd31-46e8-8f1a-64009c7febe5', 1, '2026-10-23', 2),
       (6, 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 1, '2026-10-24', 5);

-- 記録4は2枚（並び順と入れた順番を逆にして、ORDER BY sort_order が効いていることを確かめる）、記録3は1枚、
-- ほかの人の記録6は1枚。記録1・2・5は写真なし
INSERT INTO record_photos (record_id, photo_path, sort_order)
VALUES (4, '20b622c8-dd31-46e8-8f1a-64009c7febe5/records/r4-second.webp', 1),
       (4, '20b622c8-dd31-46e8-8f1a-64009c7febe5/records/r4-first.webp', 0),
       (3, '20b622c8-dd31-46e8-8f1a-64009c7febe5/records/r3-first.webp', 0),
       (6, 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa/records/r6-first.webp', 0);
