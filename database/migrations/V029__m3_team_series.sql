SET NAMES utf8mb4;

CREATE TABLE `team_series` (
    `id` BIGINT UNSIGNED NOT NULL,
    `team_id` BIGINT UNSIGNED NOT NULL,
    `created_by_user_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(160) NOT NULL,
    `slug` VARCHAR(160) NOT NULL,
    `summary` VARCHAR(1000) NULL,
    `cover_file_id` BIGINT UNSIGNED NULL,
    `serialization_status` VARCHAR(16) NOT NULL DEFAULT 'ONGOING',
    `review_status` VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    `reviewer_admin_id` BIGINT UNSIGNED NULL,
    `review_comment` VARCHAR(1000) NULL,
    `reviewed_at` DATETIME(3) NULL,
    `published_at` DATETIME(3) NULL,
    `lock_version` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_series_team_slug` (`team_id`, `slug`),
    KEY `idx_team_series_public` (`review_status`, `serialization_status`, `published_at`),
    CONSTRAINT `fk_team_series_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_team_series_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_team_series_serialization` CHECK (`serialization_status` IN ('ONGOING', 'COMPLETED', 'PAUSED')),
    CONSTRAINT `chk_team_series_review` CHECK (`review_status` IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Team article series and serialization state';

CREATE TABLE `team_series_article` (
    `id` BIGINT UNSIGNED NOT NULL,
    `series_id` BIGINT UNSIGNED NOT NULL,
    `article_id` BIGINT UNSIGNED NOT NULL,
    `chapter_order` INT NOT NULL,
    `added_by_user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_series_article_article` (`article_id`),
    UNIQUE KEY `uk_team_series_article_order` (`series_id`, `chapter_order`),
    KEY `idx_team_series_article_series` (`series_id`, `chapter_order`),
    CONSTRAINT `fk_team_series_article_series` FOREIGN KEY (`series_id`) REFERENCES `team_series` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_team_series_article_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_team_series_article_actor` FOREIGN KEY (`added_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_team_series_article_order` CHECK (`chapter_order` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Ordered article membership of a team series';

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`, `icon`, `sort`, `visible`, `status`, `is_frame`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`)
VALUES (9142, 9140, '系列审核', 2, '/community/series', '/community/series/index', 'community:series:review', 'AlbumsOutline', 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `component` = VALUES(`component`), `permission` = VALUES(`permission`), `visible` = VALUES(`visible`), `deleted` = 0, `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT `id`, 9142 FROM `sys_role`
WHERE `code` = 'admin' AND `deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = `sys_role`.`id` AND `menu_id` = 9142);
