-- insert-items.sql と一緒に使う。商品の候補は記録が1件以上ある商品だけを返すため、記録を付ける。
INSERT INTO users (id, nickname)
VALUES ('20b622c8-dd31-46e8-8f1a-64009c7febe5', 'Saki');

-- 記録がない商品（候補に出ないこと・自動削除されることの確認用）
INSERT INTO items (id, shop_id, item_name, category_id, is_seasonal)
VALUES (4, 1, 'からあげ串', 4, false);

INSERT INTO records (user_id, item_id, eaten_on, rating)
VALUES ('20b622c8-dd31-46e8-8f1a-64009c7febe5', 1, '2026-09-27', 4),
       ('20b622c8-dd31-46e8-8f1a-64009c7febe5', 2, '2026-09-27', 3),
       ('20b622c8-dd31-46e8-8f1a-64009c7febe5', 3, '2026-09-27', 5);
