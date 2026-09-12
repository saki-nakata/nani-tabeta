-- ============================================================
-- 今日なに食べた？ 初期スキーマ
-- ============================================================

-- 利用者
CREATE TABLE users (
  id                CHAR(36)     NOT NULL COMMENT 'Supabase Auth の sub',
  nickname          VARCHAR(30)  NOT NULL,
  bio               VARCHAR(200) NULL,
  profile_photo_url VARCHAR(500) NULL,
  role              VARCHAR(10)  NOT NULL DEFAULT 'user',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT chk_users_role CHECK (role IN ('user', 'admin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 分類
CREATE TABLE categories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(20) NOT NULL UNIQUE,
    category_emoji VARCHAR(8) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 業態
CREATE TABLE shop_kinds (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shop_kind_name VARCHAR(20) NOT NULL UNIQUE,
  shop_kind_emoji VARCHAR(8) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 地域
CREATE TABLE regions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  region_name VARCHAR(20) NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- エリア
CREATE TABLE areas (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  region_id BIGINT UNSIGNED NOT NULL,
  area_name VARCHAR(20) NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_areas_region FOREIGN KEY (region_id) REFERENCES regions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- 店
CREATE TABLE shops (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shop_name    VARCHAR(100)    NOT NULL,
  area_id      BIGINT UNSIGNED NOT NULL,
  shop_kind_id BIGINT UNSIGNED NOT NULL,
  is_closed    BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '閉店したか',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_shops_name_area UNIQUE (shop_name, area_id),
  CONSTRAINT fk_shops_area FOREIGN KEY (area_id)      REFERENCES areas(id),
  CONSTRAINT fk_shops_kind FOREIGN KEY (shop_kind_id) REFERENCES shop_kinds(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 商品
CREATE TABLE items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shop_id BIGINT UNSIGNED NOT NULL,
  item_name VARCHAR(100) NOT NULL,
  category_id BIGINT UNSIGNED NOT NULL,
  is_seasonal BOOLEAN NOT NULL DEFAULT FALSE COMMENT '季節限定か',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_items_shop_name UNIQUE (shop_id, item_name),
  CONSTRAINT fk_items_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
  CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories(id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 記録
 CREATE TABLE records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id CHAR(36) NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  eaten_on DATE NOT NULL,
  custom_note VARCHAR(500) NULL,
  review VARCHAR(1000) NULL,
  rating TINYINT UNSIGNED NOT NULL,
  price INT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id),
  INDEX idx_records_user_eaten (user_id, eaten_on),
  INDEX idx_records_created (created_at),
  CONSTRAINT fk_records_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_records_item FOREIGN KEY (item_id) REFERENCES items(id),
  CONSTRAINT chk_records_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 記録の写真
CREATE TABLE record_photos (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  record_id BIGINT UNSIGNED NOT NULL,
  photo_url VARCHAR(500) NOT NULL,
  sort_order TINYINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id),
  CONSTRAINT fk_record_photos_record FOREIGN KEY (record_id) REFERENCES records(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 店の写真
CREATE TABLE shop_photos (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shop_id       BIGINT UNSIGNED NOT NULL,
  user_id       CHAR(36)        NOT NULL,
  photo_url     VARCHAR(500)    NOT NULL,
  photo_type    VARCHAR(10)     NOT NULL COMMENT 'menu / shopfront',
  taken_on      DATE            NOT NULL COMMENT '撮影日',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  shopfront_key VARCHAR(80) GENERATED ALWAYS AS (
                  IF(photo_type = 'shopfront', CONCAT(shop_id, ':', user_id), NULL)
                ) STORED,
  PRIMARY KEY (id),
  INDEX idx_shop_photos_shop_type_taken (shop_id, photo_type, taken_on),
  CONSTRAINT uk_shop_photos_shopfront UNIQUE (shopfront_key),
  CONSTRAINT chk_shop_photos_type CHECK (photo_type IN ('menu', 'shopfront')),
  CONSTRAINT fk_shop_photos_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
  CONSTRAINT fk_shop_photos_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- いいね
CREATE TABLE likes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  record_id BIGINT UNSIGNED NOT NULL,
  user_id CHAR(36) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id),
  INDEX idx_likes_user_created (user_id, created_at),
  CONSTRAINT uk_likes_record_user UNIQUE (record_id, user_id),
  CONSTRAINT fk_likes_record FOREIGN KEY (record_id) REFERENCES records(id) ON DELETE CASCADE,
  CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users(id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- コメント
CREATE TABLE comments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  record_id BIGINT UNSIGNED NOT NULL,
  user_id CHAR(36) NOT NULL,
  comment_text VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id),
  CONSTRAINT fk_comments_record FOREIGN KEY (record_id) REFERENCES records(id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;