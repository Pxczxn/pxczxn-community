/*
 * Version: V007
 * Purpose: M2 关注、动态、评论、点赞、收藏与收藏夹基础结构
 * Depends on: V001-V006
 * Locking: CREATE TABLE 获取元数据锁；在维护窗口执行
 * Historical data migration: 无
 * Backup required: 已部署环境执行前必须完成数据库备份
 * Repeatable: 是，全部表使用 CREATE TABLE IF NOT EXISTS
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS `community_follow` (
    `id` BIGINT UNSIGNED NOT NULL,
    `follower_user_id` BIGINT UNSIGNED NOT NULL,
    `target_type` VARCHAR(24) NOT NULL COMMENT 'BLOG/PLATFORM_TAG/SERIES',
    `target_id` BIGINT UNSIGNED NOT NULL,
    `notification_level` VARCHAR(16) NOT NULL DEFAULT 'ALL',
    `special_follow` TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '仅个人博客允许特别关注',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_community_follow_target`
        (`follower_user_id`, `target_type`, `target_id`),
    KEY `idx_community_follow_target_created`
        (`target_type`, `target_id`, `created_at`, `id`),
    KEY `idx_community_follow_user_created`
        (`follower_user_id`, `created_at`, `id`),
    CONSTRAINT `fk_community_follow_user`
        FOREIGN KEY (`follower_user_id`)
        REFERENCES `community_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_community_follow_target_type`
        CHECK (`target_type` IN ('BLOG', 'PLATFORM_TAG', 'SERIES')),
    CONSTRAINT `chk_community_follow_notification`
        CHECK (`notification_level` IN ('ALL', 'IMPORTANT', 'MUTED')),
    CONSTRAINT `chk_community_follow_special`
        CHECK (`special_follow` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户对博客、平台标签和系列的关注关系';

CREATE TABLE IF NOT EXISTS `community_moment` (
    `id` BIGINT UNSIGNED NOT NULL,
    `actor_user_id` BIGINT UNSIGNED NOT NULL COMMENT '真实操作者',
    `blog_id` BIGINT UNSIGNED NOT NULL COMMENT '发布身份',
    `moment_type` VARCHAR(24) NOT NULL DEFAULT 'TEXT',
    `text_content` VARCHAR(4000) NULL,
    `rendered_html` MEDIUMTEXT NULL,
    `link_url` VARCHAR(2048) NULL,
    `article_id` BIGINT UNSIGNED NULL,
    `repost_moment_id` BIGINT UNSIGNED NULL,
    `visibility` VARCHAR(24) NOT NULL DEFAULT 'PUBLIC',
    `status` VARCHAR(24) NOT NULL DEFAULT 'PUBLISHED',
    `like_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `favorite_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `comment_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `repost_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_community_moment_blog_created`
        (`blog_id`, `status`, `created_at`, `id`),
    KEY `idx_community_moment_actor_created`
        (`actor_user_id`, `created_at`, `id`),
    KEY `idx_community_moment_article` (`article_id`),
    CONSTRAINT `fk_community_moment_actor`
        FOREIGN KEY (`actor_user_id`)
        REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_community_moment_blog`
        FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_community_moment_article`
        FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_community_moment_repost`
        FOREIGN KEY (`repost_moment_id`)
        REFERENCES `community_moment` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_community_moment_type`
        CHECK (`moment_type` IN (
            'TEXT', 'IMAGE', 'LINK', 'ARTICLE_SHARE', 'PROJECT_UPDATE',
            'CODE', 'POLL', 'TEAM_NOTICE', 'REPOST', 'QUOTE', 'VIDEO_LINK'
        )),
    CONSTRAINT `chk_community_moment_visibility`
        CHECK (`visibility` IN ('PUBLIC', 'FOLLOWERS_ONLY', 'PRIVATE', 'UNLISTED')),
    CONSTRAINT `chk_community_moment_status`
        CHECK (`status` IN (
            'PENDING_REVIEW', 'PUBLISHED', 'HIDDEN', 'TAKEN_DOWN', 'DELETED'
        ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='个人或团队博客身份发布的社区动态';

CREATE TABLE IF NOT EXISTS `community_comment` (
    `id` BIGINT UNSIGNED NOT NULL,
    `author_user_id` BIGINT UNSIGNED NOT NULL,
    `target_type` VARCHAR(16) NOT NULL COMMENT 'ARTICLE/MOMENT',
    `target_id` BIGINT UNSIGNED NOT NULL,
    `root_comment_id` BIGINT UNSIGNED NULL,
    `parent_comment_id` BIGINT UNSIGNED NULL,
    `reply_to_user_id` BIGINT UNSIGNED NULL,
    `content_text` VARCHAR(2000) NOT NULL,
    `rendered_html` TEXT NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    `like_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_community_comment_target`
        (`target_type`, `target_id`, `status`, `created_at`, `id`),
    KEY `idx_community_comment_root`
        (`root_comment_id`, `created_at`, `id`),
    KEY `idx_community_comment_author`
        (`author_user_id`, `created_at`, `id`),
    CONSTRAINT `fk_community_comment_author`
        FOREIGN KEY (`author_user_id`)
        REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_community_comment_root`
        FOREIGN KEY (`root_comment_id`)
        REFERENCES `community_comment` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_community_comment_parent`
        FOREIGN KEY (`parent_comment_id`)
        REFERENCES `community_comment` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_community_comment_reply_user`
        FOREIGN KEY (`reply_to_user_id`)
        REFERENCES `community_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_community_comment_target`
        CHECK (`target_type` IN ('ARTICLE', 'MOMENT')),
    CONSTRAINT `chk_community_comment_status`
        CHECK (`status` IN (
            'PENDING_REVIEW', 'PUBLISHED', 'HIDDEN_BY_AUTHOR',
            'HIDDEN_BY_BLOG', 'DELETED_BY_USER', 'TAKEN_DOWN', 'SPAM'
        ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='文章与动态的两层评论和回复';

CREATE TABLE IF NOT EXISTS `community_content_like` (
    `id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `target_type` VARCHAR(16) NOT NULL COMMENT 'ARTICLE/MOMENT/COMMENT',
    `target_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_content_like_user_target`
        (`user_id`, `target_type`, `target_id`),
    KEY `idx_content_like_target_created`
        (`target_type`, `target_id`, `created_at`, `id`),
    CONSTRAINT `fk_content_like_user`
        FOREIGN KEY (`user_id`)
        REFERENCES `community_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_content_like_target`
        CHECK (`target_type` IN ('ARTICLE', 'MOMENT', 'COMMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='文章、动态、评论与回复点赞关系';

CREATE TABLE IF NOT EXISTS `favorite_folder` (
    `id` BIGINT UNSIGNED NOT NULL,
    `owner_user_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(80) NOT NULL,
    `description` VARCHAR(300) NULL,
    `visibility` VARCHAR(24) NOT NULL DEFAULT 'PRIVATE',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0,
    `item_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    `active_name` VARCHAR(80)
        GENERATED ALWAYS AS (
            CASE WHEN `deleted_at` IS NULL THEN `name` ELSE NULL END
        ) STORED,
    `default_owner_id` BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE
                WHEN `is_default` = 1 AND `deleted_at` IS NULL
                THEN `owner_user_id`
                ELSE NULL
            END
        ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite_folder_name` (`owner_user_id`, `active_name`),
    UNIQUE KEY `uk_favorite_folder_default` (`default_owner_id`),
    KEY `idx_favorite_folder_owner_sort`
        (`owner_user_id`, `sort_order`, `id`),
    CONSTRAINT `fk_favorite_folder_owner`
        FOREIGN KEY (`owner_user_id`)
        REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_favorite_folder_visibility`
        CHECK (`visibility` IN (
            'PRIVATE', 'PUBLIC', 'FOLLOWERS_ONLY', 'MUTUAL_ONLY'
        )),
    CONSTRAINT `chk_favorite_folder_default`
        CHECK (`is_default` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户自定义收藏夹';

CREATE TABLE IF NOT EXISTS `favorite_item` (
    `id` BIGINT UNSIGNED NOT NULL,
    `owner_user_id` BIGINT UNSIGNED NOT NULL,
    `target_type` VARCHAR(16) NOT NULL COMMENT 'ARTICLE/MOMENT',
    `target_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite_item_owner_target`
        (`owner_user_id`, `target_type`, `target_id`),
    KEY `idx_favorite_item_owner_created`
        (`owner_user_id`, `created_at`, `id`),
    KEY `idx_favorite_item_target`
        (`target_type`, `target_id`, `created_at`, `id`),
    CONSTRAINT `fk_favorite_item_owner`
        FOREIGN KEY (`owner_user_id`)
        REFERENCES `community_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_favorite_item_target`
        CHECK (`target_type` IN ('ARTICLE', 'MOMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户收藏内容的唯一关系';

CREATE TABLE IF NOT EXISTS `favorite_folder_item` (
    `id` BIGINT UNSIGNED NOT NULL,
    `folder_id` BIGINT UNSIGNED NOT NULL,
    `favorite_item_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite_folder_item`
        (`folder_id`, `favorite_item_id`),
    KEY `idx_favorite_folder_item_content` (`favorite_item_id`),
    CONSTRAINT `fk_favorite_folder_item_folder`
        FOREIGN KEY (`folder_id`)
        REFERENCES `favorite_folder` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_favorite_folder_item_content`
        FOREIGN KEY (`favorite_item_id`)
        REFERENCES `favorite_item` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='同一收藏内容加入多个收藏夹的映射';
