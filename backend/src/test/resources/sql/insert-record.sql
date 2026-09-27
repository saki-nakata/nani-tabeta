INSERT INTO users (id, nickname)
VALUES ('20b622c8-dd31-46e8-8f1a-64009c7febe5', 'Saki');

INSERT INTO shops (id, shop_name, area_id, shop_kind_id, is_closed)
VALUES (1, 'セブンイレブン', 20, 1, false);

INSERT INTO items (id, shop_id, item_name, category_id, is_seasonal)
VALUES (1, 1, 'からあげ棒', 4, false);

INSERT INTO records (id, user_id, item_id, eaten_on, custom_note, review, rating, price)
VALUES (1, '20b622c8-dd31-46e8-8f1a-64009c7febe5', 1, '2026-09-27', '辛さ多め', '衣がサクサク', 4, 220);

-- 並び順と入れた順番を逆にして、ORDER BY sort_order が効いていることを確かめる
INSERT INTO record_photos (record_id, photo_path, sort_order)
VALUES (1, '20b622c8-dd31-46e8-8f1a-64009c7febe5/records/second.webp', 1),
       (1, '20b622c8-dd31-46e8-8f1a-64009c7febe5/records/first.webp', 0);
