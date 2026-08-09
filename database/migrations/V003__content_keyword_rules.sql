/*
 * Version: V003
 * Purpose: Persist configurable keyword rules used by automatic content review.
 * Depends on: V001 community schema.
 * Locking: CREATE TABLE takes a metadata lock; run during a maintenance window.
 * Repeatable: Yes when an existing table has the same reviewed structure.
 *
 * No production keyword is seeded here. Rules are policy data and must be
 * approved for the deployment jurisdiction before insertion.
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS `content_keyword_rule` (
    `id` BIGINT UNSIGNED NOT NULL,
    `keyword` VARCHAR(200) NOT NULL COMMENT 'Original policy phrase',
    `normalized_keyword` VARCHAR(200) NOT NULL COMMENT 'NFKC/lowercase matching form',
    `severity` VARCHAR(16) NOT NULL COMMENT 'BLOCK / REVIEW / WARN',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    `description` VARCHAR(500) NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_by_admin_id` BIGINT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    `active_normalized_keyword` VARCHAR(200)
        GENERATED ALWAYS AS (
            CASE
                WHEN `status` <> 'DELETED' AND `deleted_at` IS NULL
                THEN `normalized_keyword`
                ELSE NULL
            END
        ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword_rule_active_normalized` (`active_normalized_keyword`),
    KEY `idx_keyword_rule_scan` (`status`, `severity`, `sort_order`, `id`),
    CONSTRAINT `chk_keyword_rule_severity`
        CHECK (`severity` IN ('BLOCK', 'REVIEW', 'WARN')),
    CONSTRAINT `chk_keyword_rule_status`
        CHECK (`status` IN ('ACTIVE', 'DISABLED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Configurable keyword rules for content moderation';
