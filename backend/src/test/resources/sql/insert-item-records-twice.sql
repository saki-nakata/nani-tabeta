-- insert-items.sql と insert-item-records.sql と一緒に使う。
-- 商品 1（からあげ棒）に記録をもう 1 件足し、記録が複数あっても商品が 1 行だけ返ることを確かめる。
INSERT INTO records (user_id, item_id, eaten_on, rating)
VALUES ('20b622c8-dd31-46e8-8f1a-64009c7febe5', 1, '2026-10-01', 5);
