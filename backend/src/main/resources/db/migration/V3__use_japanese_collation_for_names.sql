-- 店名・商品名は、濁点・半濁点と大文字小文字を区別する照合順序にする。
-- utf8mb4_0900_ai_ci では「パン」と「バン」が同じと判定され、
-- UNIQUE 制約と同じ店・商品の判定で、別の店・商品が1つにまとめられてしまうため。
ALTER TABLE shops MODIFY shop_name VARCHAR(100) NOT NULL COLLATE utf8mb4_ja_0900_as_cs;
ALTER TABLE items MODIFY item_name VARCHAR(100) NOT NULL COLLATE utf8mb4_ja_0900_as_cs;
