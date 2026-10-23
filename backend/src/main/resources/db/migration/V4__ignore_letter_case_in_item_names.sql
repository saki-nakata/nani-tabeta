-- 商品名は、同じ店の中で英字の大文字小文字だけが違う名前（Latte と latte）を、同じ商品として扱う。
-- MySQL の LOWER() で小文字にしてから、列の照合順序（utf8mb4_ja_0900_as_cs）で比べる。
-- 照合順序を utf8mb4_0900_as_ci に変える方法は使わない。小さいかな（きゃ と きや）や半角カナ、① と 1 まで
-- 同じと判定され、別の商品が1つにまとめられてしまうため。
-- 店名は変えない。同じ都道府県に、書き方だけが違う別の店がありうるため。

-- 外部キー fk_items_shop は、shop_id のインデックスとして uk_items_shop_name を使っている。
-- 消すだけの命令を先に流すと「外部キーに必要」として拒否されるので、消すのと作り直すのを1つの ALTER TABLE で行う。
-- 新しい UNIQUE も先頭が shop_id なので、外部キーはそのまま新しい UNIQUE を使う。
ALTER TABLE items
  DROP INDEX uk_items_shop_name,
  ADD UNIQUE KEY uk_items_shop_name (shop_id, (LOWER(item_name)));
