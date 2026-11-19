-- 記録の版（編集の競合を防ぐ番号）
-- 編集を保存するたびに 1 増やし、保存のときに「画面が見た版」と今の版が同じときだけ更新する
-- （2 つの画面で同じ記録を編集したとき、古い画面の内容で上書きしないため）
ALTER TABLE records
  ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 AFTER price;
