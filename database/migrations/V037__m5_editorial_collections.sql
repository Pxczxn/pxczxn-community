/* M5-T004: operator-managed topics, events, announcements and curated collections. */
SET NAMES utf8mb4;
CREATE TABLE `editorial_collection` (
  `id` BIGINT UNSIGNED NOT NULL, `kind` VARCHAR(24) NOT NULL, `title` VARCHAR(160) NOT NULL,
  `slug` VARCHAR(160) NOT NULL, `summary` VARCHAR(1000) NULL, `cover_file_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT', `starts_at` DATETIME(3) NULL, `ends_at` DATETIME(3) NULL,
  `display_order` INT NOT NULL DEFAULT 0, `created_by_admin_id` BIGINT NULL, `published_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`), UNIQUE KEY `uk_editorial_collection_slug` (`slug`), KEY `idx_editorial_public` (`status`,`starts_at`,`ends_at`,`display_order`,`published_at`),
  CONSTRAINT `chk_editorial_kind` CHECK (`kind` IN ('TOPIC','EVENT','ANNOUNCEMENT','FEATURED','COLLECTION')),
  CONSTRAINT `chk_editorial_status` CHECK (`status` IN ('DRAFT','PUBLISHED','ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `editorial_collection_item` (
  `id` BIGINT UNSIGNED NOT NULL, `collection_id` BIGINT UNSIGNED NOT NULL, `target_type` VARCHAR(16) NOT NULL, `target_id` BIGINT UNSIGNED NOT NULL,
  `display_order` INT NOT NULL DEFAULT 0, `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`), UNIQUE KEY `uk_editorial_item_target` (`collection_id`,`target_type`,`target_id`), KEY `idx_editorial_item_order` (`collection_id`,`display_order`,`id`),
  CONSTRAINT `fk_editorial_item_collection` FOREIGN KEY (`collection_id`) REFERENCES `editorial_collection` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_editorial_item_type` CHECK (`target_type` IN ('ARTICLE','SERIES'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`) VALUES
  (9140,9110,'运营内容',2,'/community/editorial','/community/editorial/index','community:editorial:list','Star',40,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
  (9141,9140,'管理运营内容',3,NULL,NULL,'community:editorial:manage',NULL,1,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0)
ON DUPLICATE KEY UPDATE `permission`=VALUES(`permission`),`update_time`=CURRENT_TIMESTAMP;
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) SELECT r.id,v.menu_id FROM `sys_role` r CROSS JOIN (SELECT 9140 menu_id UNION ALL SELECT 9141) v WHERE r.code='admin' AND r.deleted=0 AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` e WHERE e.role_id=r.id AND e.menu_id=v.menu_id);
