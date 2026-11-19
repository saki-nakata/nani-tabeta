-- RecordVersionConflictTest 専用のデータ（insert-record-transaction.sql の利用者・店のあとに入れる）
-- このテストはトランザクションを確定させるため、ほかのテストとぶつからない ID を使い、終わったら
-- delete-record-transaction.sql で削除する（利用者 1111… の記録と写真、店 9001 の商品）
INSERT INTO items (id, shop_id, item_name, category_id, is_seasonal)
VALUES (9101, 9001, 'からあげ棒', 4, false);

-- 版 0 の記録。写真は p・q の 2 枚
INSERT INTO records (id, user_id, item_id, eaten_on, custom_note, review, rating, price, version)
VALUES (9201, '11111111-1111-1111-1111-111111111111', 9101, '2026-09-27', NULL, '編集前', 3, NULL, 0);

INSERT INTO record_photos (record_id, photo_path, sort_order)
VALUES (9201, '11111111-1111-1111-1111-111111111111/records/aaaaaaaa-0000-4000-8000-000000000001.webp', 0),
       (9201, '11111111-1111-1111-1111-111111111111/records/aaaaaaaa-0000-4000-8000-000000000002.webp', 1);
