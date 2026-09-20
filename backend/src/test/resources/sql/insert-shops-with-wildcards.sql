-- LIKE のワイルドカード（% _）を名前に含む店と、含まない店を用意する。
-- エスケープが効いていない場合、ワイルドカードが働いて 100XYOFF まで一致してしまう。
INSERT INTO shops (id, shop_name, area_id, shop_kind_id, is_closed)
VALUES (1, '100%_OFF', 20, 1, false),
       (2, '100XYOFF', 20, 1, false);
