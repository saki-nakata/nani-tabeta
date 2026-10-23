-- 店名は、ひらがなとカタカナだけが違う名前（さくら と サクラ）も、別の店として扱う。
-- 同じ都道府県に、書き方だけが違う別の店がありうるため。
-- utf8mb4_ja_0900_as_cs_ks は、今の utf8mb4_ja_0900_as_cs に「かなの区別」だけを足した照合順序。
-- 濁点・大文字小文字・小さいかな・半角カナは、今までどおり区別する。
-- 商品名は変えない（ひらがなとカタカナの違いは、同じ商品として扱ったまま）。
-- 区別を増やす変更なので、今のデータで UNIQUE (shop_name, area_id) の重複が増えることはない。
ALTER TABLE shops MODIFY shop_name VARCHAR(100) NOT NULL COLLATE utf8mb4_ja_0900_as_cs_ks;
