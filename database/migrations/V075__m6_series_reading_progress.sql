SET NAMES utf8mb4;

-- ======================================================================
-- V075: 系列轻量阅读进度
-- 只记录「最后阅读位置」与「最远进度」两个事实，不保存已读章节集合：
--   last_article_id / last_chapter_order -> 用于「继续阅读」定位
--   max_chapter_order                    -> 用于进度条百分比与连载追更提示
-- 顺序连载场景下 max_chapter_order 即已读章节数，回看旧章节不会倒退进度。
-- ======================================================================

CREATE TABLE `series_reading_progress` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Reader user ID',
    `series_id` BIGINT UNSIGNED NOT NULL COMMENT 'Series ID',
    `last_article_id` BIGINT UNSIGNED NULL COMMENT 'Last read chapter article ID',
    `last_chapter_order` INT NOT NULL DEFAULT 0 COMMENT 'Last read chapter order',
    `max_chapter_order` INT NOT NULL DEFAULT 0 COMMENT 'Farthest chapter order reached',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'First read time',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Last read time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_series_reading_progress_user_series` (`user_id`, `series_id`),
    KEY `idx_series_reading_progress_series` (`series_id`),
    KEY `idx_series_reading_progress_recent` (`user_id`, `updated_at`),
    CONSTRAINT `fk_series_reading_progress_user` FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_series_reading_progress_series` FOREIGN KEY (`series_id`) REFERENCES `series` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_series_reading_progress_article` FOREIGN KEY (`last_article_id`) REFERENCES `article` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_series_reading_progress_order` CHECK (`last_chapter_order` >= 0 AND `max_chapter_order` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Series reading progress';
