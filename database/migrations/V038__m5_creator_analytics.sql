/* M5-T007: privacy-minimised creator analytics. No IP, user agent or visitor identity is stored. */
SET NAMES utf8mb4;
CREATE TABLE `creator_analytics_event` (
  `id` BIGINT UNSIGNED NOT NULL, `article_id` BIGINT UNSIGNED NOT NULL, `author_user_id` BIGINT UNSIGNED NOT NULL,
  `event_type` VARCHAR(24) NOT NULL, `source_type` VARCHAR(24) NOT NULL DEFAULT 'DIRECT', `search_term` VARCHAR(80) NULL,
  `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`), KEY `idx_creator_analytics_author_day` (`author_user_id`,`occurred_at`), KEY `idx_creator_analytics_article_day` (`article_id`,`occurred_at`),
  CONSTRAINT `chk_creator_analytics_type` CHECK (`event_type` IN ('VIEW','SEARCH_CLICK')),
  CONSTRAINT `chk_creator_analytics_source` CHECK (`source_type` IN ('DIRECT','INTERNAL','SEARCH','REFERRAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
