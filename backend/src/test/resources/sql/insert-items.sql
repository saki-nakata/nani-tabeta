INSERT INTO shops (id, shop_name, area_id, shop_kind_id, is_closed)
VALUES (1, 'セブンイレブン', 20, 1, false),
       (2, 'ファミリーマート', 20, 1, false),
       (3, 'ローソン', 20, 1, false);

INSERT INTO items (id, shop_id, item_name, category_id, is_seasonal)
VALUES (1, 1, 'からあげ棒', 4, false),
       (2, 1, 'おにぎり 鮭', 1, false),
       (3, 2, 'からあげ棒', 4, false);
