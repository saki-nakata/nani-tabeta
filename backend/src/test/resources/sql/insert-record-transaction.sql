-- RecordServiceTransactionTest 専用のデータ。
-- このテストはトランザクションを確定させるため、ほかのテストとぶつからない ID を使い、終わったら削除する。
INSERT INTO users (id, nickname)
VALUES ('11111111-1111-1111-1111-111111111111', 'トランザクションテスト');

INSERT INTO shops (id, shop_name, area_id, shop_kind_id, is_closed)
VALUES (9001, 'トランザクションテストの店', 20, 1, false);
