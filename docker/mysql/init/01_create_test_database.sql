-- テスト専用のデータベース（初回起動時にだけ実行される）
CREATE DATABASE IF NOT EXISTS nani_tabeta_test
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

-- アプリ用ユーザーに、テスト用DBの権限を付ける
GRANT ALL PRIVILEGES ON `nani\_tabeta\_test`.* TO 'nani_tabeta'@'%';