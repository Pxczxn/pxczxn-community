/*
 * Version: V001
 * Purpose: M1 社区用户、个人博客、文章版本、审核、通知与文件引用基础结构
 * Depends on: 后台系统基础表已经初始化；当前连接已选择目标数据库
 * Locking: CREATE TABLE 仅获取元数据锁；新库或空闲维护窗口执行
 * Historical data migration: 无
 * Backup required: 已部署环境执行前必须完成数据库备份
 * Repeatable: 是，全部业务表使用 CREATE TABLE IF NOT EXISTS
 *
 * 时间约定：应用以 UTC 写入所有 DATETIME(3) 字段。
 * ID 约定：BIGINT UNSIGNED 由后端生成，不使用 AUTO_INCREMENT。
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS `community_user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '社区用户 ID',
    `username` VARCHAR(32) NOT NULL COMMENT '可修改的公开用户名',
    `display_name` VARCHAR(80) NOT NULL COMMENT '公开显示名称',
    `bio` VARCHAR(500) NULL COMMENT '公开简介',
    `avatar_file_id` BIGINT UNSIGNED NULL COMMENT '头像文件 ID',
    `status` VARCHAR(24) NOT NULL DEFAULT 'NORMAL' COMMENT '账号状态',
    `personal_blog_id` BIGINT UNSIGNED NULL COMMENT '唯一的个人博客 ID',
    `verification_status` VARCHAR(24) NOT NULL DEFAULT 'UNVERIFIED' COMMENT '认证状态',
    `publish_restricted_until` DATETIME(3) NULL COMMENT '禁止发布截止时间',
    `comment_restricted_until` DATETIME(3) NULL COMMENT '禁止评论截止时间',
    `last_login_at` DATETIME(3) NULL,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_community_user_username` (`username`),
    UNIQUE KEY `uk_community_user_personal_blog` (`personal_blog_id`),
    KEY `idx_community_user_status_created` (`status`, `created_at`),
    CONSTRAINT `chk_community_user_status`
        CHECK (`status` IN ('NORMAL', 'LIMITED', 'FROZEN', 'BANNED', 'DEACTIVATED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='社区用户账号主体';

CREATE TABLE IF NOT EXISTS `community_user_login_account` (
    `id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `login_type` VARCHAR(24) NOT NULL DEFAULT 'EMAIL',
    `normalized_identifier` VARCHAR(320) NOT NULL COMMENT '规范化后的邮箱等登录标识',
    `password_hash` VARCHAR(255) NULL COMMENT 'BCrypt 或 Argon2id 哈希',
    `verified_at` DATETIME(3) NULL,
    `failed_login_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `locked_until` DATETIME(3) NULL,
    `last_login_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_login_account_type_identifier` (`login_type`, `normalized_identifier`),
    UNIQUE KEY `uk_login_account_user_type` (`user_id`, `login_type`),
    CONSTRAINT `fk_login_account_user`
        FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_login_account_type`
        CHECK (`login_type` IN ('EMAIL', 'PHONE', 'GITHUB', 'WECHAT', 'GOOGLE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='社区用户登录账号';

CREATE TABLE IF NOT EXISTS `community_user_preference` (
    `id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `locale` VARCHAR(16) NOT NULL DEFAULT 'zh-CN',
    `time_zone` VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    `email_notification_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `content_language` VARCHAR(16) NOT NULL DEFAULT 'zh-CN',
    `settings_json` JSON NULL COMMENT '非关系型界面偏好',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_preference_user` (`user_id`),
    CONSTRAINT `fk_user_preference_user`
        FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='社区用户偏好';

CREATE TABLE IF NOT EXISTS `blog` (
    `id` BIGINT UNSIGNED NOT NULL,
    `blog_type` VARCHAR(16) NOT NULL COMMENT 'PERSONAL 或 TEAM',
    `owner_user_id` BIGINT UNSIGNED NOT NULL COMMENT '个人博主或团队发起人',
    `name` VARCHAR(120) NOT NULL,
    `slug` VARCHAR(80) NOT NULL,
    `summary` VARCHAR(500) NULL,
    `avatar_file_id` BIGINT UNSIGNED NULL,
    `background_file_id` BIGINT UNSIGNED NULL,
    `status` VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    `article_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `follower_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    `personal_owner_user_id` BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE WHEN `blog_type` = 'PERSONAL' THEN `owner_user_id` ELSE NULL END
        ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_slug` (`slug`),
    UNIQUE KEY `uk_blog_personal_owner` (`personal_owner_user_id`),
    KEY `idx_blog_owner_type` (`owner_user_id`, `blog_type`),
    KEY `idx_blog_status_created` (`status`, `created_at`),
    CONSTRAINT `fk_blog_owner_user`
        FOREIGN KEY (`owner_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_blog_type` CHECK (`blog_type` IN ('PERSONAL', 'TEAM')),
    CONSTRAINT `chk_blog_status`
        CHECK (`status` IN ('ACTIVE', 'HIDDEN', 'FROZEN', 'CLOSED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='个人博客与团队博客';

CREATE TABLE IF NOT EXISTS `blog_setting` (
    `id` BIGINT UNSIGNED NOT NULL,
    `blog_id` BIGINT UNSIGNED NOT NULL,
    `comment_scope` VARCHAR(32) NOT NULL DEFAULT 'ALL_LOGGED_IN',
    `default_visibility` VARCHAR(24) NOT NULL DEFAULT 'PUBLIC',
    `allow_repost` VARCHAR(24) NOT NULL DEFAULT 'ALLOW',
    `theme_key` VARCHAR(64) NOT NULL DEFAULT 'default',
    `theme_config_json` JSON NULL COMMENT '安全主题配置，不允许任意 CSS/JS',
    `seo_title` VARCHAR(160) NULL,
    `seo_description` VARCHAR(300) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_setting_blog` (`blog_id`),
    CONSTRAINT `fk_blog_setting_blog`
        FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_blog_setting_visibility`
        CHECK (`default_visibility` IN ('PUBLIC', 'PRIVATE', 'FOLLOWERS_ONLY', 'UNLISTED')),
    CONSTRAINT `chk_blog_setting_repost`
        CHECK (`allow_repost` IN ('ALLOW', 'APPROVAL_REQUIRED', 'DISALLOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='博客设置';

CREATE TABLE IF NOT EXISTS `blog_category` (
    `id` BIGINT UNSIGNED NOT NULL,
    `blog_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(80) NOT NULL,
    `slug` VARCHAR(80) NOT NULL,
    `description` VARCHAR(300) NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `is_default` TINYINT(1) NOT NULL DEFAULT 0,
    `article_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    `default_blog_id` BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE WHEN `is_default` = 1 AND `deleted_at` IS NULL THEN `blog_id` ELSE NULL END
        ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_category_slug` (`blog_id`, `slug`),
    UNIQUE KEY `uk_blog_category_default` (`default_blog_id`),
    KEY `idx_blog_category_sort` (`blog_id`, `sort_order`, `id`),
    CONSTRAINT `fk_blog_category_blog`
        FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_blog_category_default` CHECK (`is_default` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='博客一级分类';

CREATE TABLE IF NOT EXISTS `article` (
    `id` BIGINT UNSIGNED NOT NULL,
    `blog_id` BIGINT UNSIGNED NOT NULL,
    `author_user_id` BIGINT UNSIGNED NOT NULL,
    `category_id` BIGINT UNSIGNED NULL,
    `title` VARCHAR(200) NOT NULL,
    `slug` VARCHAR(160) NOT NULL,
    `summary` VARCHAR(500) NULL,
    `cover_file_id` BIGINT UNSIGNED NULL,
    `content_mode` VARCHAR(16) NOT NULL,
    `visibility` VARCHAR(24) NOT NULL DEFAULT 'PUBLIC',
    `publish_method` VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    `publish_status` VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    `review_status` VARCHAR(24) NOT NULL DEFAULT 'NOT_SUBMITTED',
    `current_version_id` BIGINT UNSIGNED NULL COMMENT '当前编辑版本；循环关系由应用事务维护',
    `published_version_id` BIGINT UNSIGNED NULL COMMENT '当前公开版本；循环关系由应用事务维护',
    `review_version_id` BIGINT UNSIGNED NULL COMMENT '当前审核固定版本',
    `scheduled_publish_at` DATETIME(3) NULL,
    `published_at` DATETIME(3) NULL,
    `canonical_path` VARCHAR(500) NULL,
    `view_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `like_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `favorite_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `comment_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_blog_slug` (`blog_id`, `slug`),
    KEY `idx_article_author_status` (`author_user_id`, `publish_status`, `updated_at`),
    KEY `idx_article_blog_public` (`blog_id`, `publish_status`, `visibility`, `published_at`),
    KEY `idx_article_review_queue` (`review_status`, `updated_at`),
    KEY `idx_article_category_public` (`category_id`, `publish_status`, `published_at`),
    CONSTRAINT `fk_article_blog`
        FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_article_author`
        FOREIGN KEY (`author_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_article_category`
        FOREIGN KEY (`category_id`) REFERENCES `blog_category` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_article_content_mode`
        CHECK (`content_mode` IN ('RICH_TEXT', 'MARKDOWN')),
    CONSTRAINT `chk_article_visibility`
        CHECK (`visibility` IN ('PUBLIC', 'PRIVATE', 'FOLLOWERS_ONLY', 'UNLISTED')),
    CONSTRAINT `chk_article_publish_method`
        CHECK (`publish_method` IN ('IMMEDIATE', 'SCHEDULED', 'MANUAL')),
    CONSTRAINT `chk_article_publish_status`
        CHECK (`publish_status` IN (
            'DRAFT', 'PENDING_REVIEW', 'APPROVED', 'SCHEDULED', 'PUBLISHED',
            'HIDDEN', 'TAKEN_DOWN', 'PUBLISH_FAILED', 'DELETED'
        )),
    CONSTRAINT `chk_article_review_status`
        CHECK (`review_status` IN (
            'NOT_SUBMITTED', 'QUEUED', 'AUTO_REVIEWING', 'MANUAL_REVIEWING',
            'APPROVED', 'REVISION_REQUIRED', 'REJECTED', 'CANCELLED', 'EXPIRED'
        ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='文章稳定元数据';

CREATE TABLE IF NOT EXISTS `article_version` (
    `id` BIGINT UNSIGNED NOT NULL,
    `article_id` BIGINT UNSIGNED NOT NULL,
    `version_no` INT UNSIGNED NOT NULL,
    `content_mode` VARCHAR(16) NOT NULL,
    `rich_text_json` LONGTEXT NULL COMMENT 'Tiptap/ProseMirror JSON',
    `markdown_content` LONGTEXT NULL,
    `rendered_html` LONGTEXT NOT NULL COMMENT '服务端安全渲染缓存',
    `plain_text` LONGTEXT NOT NULL,
    `toc_json` JSON NULL,
    `content_hash` CHAR(64) NOT NULL,
    `word_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `reading_time_minutes` INT UNSIGNED NOT NULL DEFAULT 1,
    `created_by_user_id` BIGINT UNSIGNED NOT NULL,
    `creation_type` VARCHAR(24) NOT NULL DEFAULT 'MANUAL_SAVE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_version_number` (`article_id`, `version_no`),
    KEY `idx_article_version_created` (`article_id`, `created_at`),
    CONSTRAINT `fk_article_version_article`
        FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_article_version_creator`
        FOREIGN KEY (`created_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_article_version_content_mode`
        CHECK (`content_mode` IN ('RICH_TEXT', 'MARKDOWN')),
    CONSTRAINT `chk_article_version_single_source`
        CHECK (
            (`content_mode` = 'RICH_TEXT' AND `rich_text_json` IS NOT NULL AND `markdown_content` IS NULL)
            OR
            (`content_mode` = 'MARKDOWN' AND `markdown_content` IS NOT NULL AND `rich_text_json` IS NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='文章完整正文快照';

CREATE TABLE IF NOT EXISTS `platform_tag` (
    `id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(80) NOT NULL,
    `slug` VARCHAR(80) NOT NULL,
    `description` VARCHAR(300) NULL,
    `status` VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    `merged_to_tag_id` BIGINT UNSIGNED NULL,
    `usage_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_by_admin_id` BIGINT NULL COMMENT '后台管理员 ID，不与社区用户混用',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_tag_name` (`name`),
    UNIQUE KEY `uk_platform_tag_slug` (`slug`),
    KEY `idx_platform_tag_status_usage` (`status`, `usage_count`),
    CONSTRAINT `fk_platform_tag_merged_to`
        FOREIGN KEY (`merged_to_tag_id`) REFERENCES `platform_tag` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_platform_tag_status`
        CHECK (`status` IN ('ACTIVE', 'HIDDEN', 'MERGED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='平台统一标签';

CREATE TABLE IF NOT EXISTS `article_tag` (
    `article_id` BIGINT UNSIGNED NOT NULL,
    `tag_id` BIGINT UNSIGNED NOT NULL,
    `sort_order` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`article_id`, `tag_id`),
    KEY `idx_article_tag_tag` (`tag_id`, `article_id`),
    CONSTRAINT `fk_article_tag_article`
        FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_article_tag_tag`
        FOREIGN KEY (`tag_id`) REFERENCES `platform_tag` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_article_tag_sort_order` CHECK (`sort_order` BETWEEN 0 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='文章与平台标签关系';

CREATE TABLE IF NOT EXISTS `content_review_task` (
    `id` BIGINT UNSIGNED NOT NULL,
    `subject_type` VARCHAR(24) NOT NULL DEFAULT 'ARTICLE',
    `subject_id` BIGINT UNSIGNED NOT NULL,
    `article_id` BIGINT UNSIGNED NOT NULL,
    `fixed_version_id` BIGINT UNSIGNED NOT NULL COMMENT '提交后不可变的审核版本',
    `review_stage` VARCHAR(24) NOT NULL DEFAULT 'PLATFORM_AUTO',
    `review_type` VARCHAR(16) NOT NULL DEFAULT 'AUTO',
    `status` VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
    `risk_level` VARCHAR(16) NOT NULL DEFAULT 'LOW',
    `idempotency_key` VARCHAR(80) NOT NULL,
    `submitted_by_user_id` BIGINT UNSIGNED NOT NULL,
    `assignee_admin_id` BIGINT NULL COMMENT '平台管理员 ID',
    `result_code` VARCHAR(64) NULL,
    `result_reason` VARCHAR(1000) NULL,
    `submitted_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `claimed_at` DATETIME(3) NULL,
    `completed_at` DATETIME(3) NULL,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `active_article_id` BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE
                WHEN `status` IN ('QUEUED', 'AUTO_REVIEWING', 'MANUAL_REVIEWING')
                THEN `article_id`
                ELSE NULL
            END
        ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_task_idempotency` (`idempotency_key`),
    UNIQUE KEY `uk_review_task_active_article` (`active_article_id`),
    KEY `idx_review_task_queue` (`status`, `risk_level`, `submitted_at`),
    KEY `idx_review_task_assignee` (`assignee_admin_id`, `status`, `claimed_at`),
    KEY `idx_review_task_article_history` (`article_id`, `submitted_at`),
    CONSTRAINT `fk_review_task_article`
        FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_review_task_version`
        FOREIGN KEY (`fixed_version_id`) REFERENCES `article_version` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_review_task_submitter`
        FOREIGN KEY (`submitted_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_review_task_stage`
        CHECK (`review_stage` IN ('TEAM_INTERNAL', 'PLATFORM_AUTO', 'PLATFORM_MANUAL', 'APPEAL_REVIEW')),
    CONSTRAINT `chk_review_task_type` CHECK (`review_type` IN ('AUTO', 'MANUAL')),
    CONSTRAINT `chk_review_task_status`
        CHECK (`status` IN (
            'QUEUED', 'AUTO_REVIEWING', 'MANUAL_REVIEWING', 'APPROVED',
            'REVISION_REQUIRED', 'REJECTED', 'CANCELLED', 'EXPIRED'
        )),
    CONSTRAINT `chk_review_task_risk`
        CHECK (`risk_level` IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='内容审核任务与不可删除审核记录';

CREATE TABLE IF NOT EXISTS `community_notification` (
    `id` BIGINT UNSIGNED NOT NULL,
    `notification_type` VARCHAR(32) NOT NULL,
    `sender_user_id` BIGINT UNSIGNED NULL,
    `title` VARCHAR(160) NOT NULL,
    `content` VARCHAR(1000) NOT NULL,
    `target_type` VARCHAR(32) NULL,
    `target_id` BIGINT UNSIGNED NULL,
    `deduplication_key` VARCHAR(120) NULL,
    `payload_json` JSON NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_notification_type_created` (`notification_type`, `created_at`),
    KEY `idx_notification_deduplication` (`deduplication_key`, `created_at`),
    CONSTRAINT `fk_notification_sender`
        FOREIGN KEY (`sender_user_id`) REFERENCES `community_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='站内通知内容';

CREATE TABLE IF NOT EXISTS `community_notification_recipient` (
    `id` BIGINT UNSIGNED NOT NULL,
    `notification_id` BIGINT UNSIGNED NOT NULL,
    `recipient_user_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'UNREAD',
    `read_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_recipient` (`notification_id`, `recipient_user_id`),
    KEY `idx_recipient_inbox` (`recipient_user_id`, `status`, `created_at`),
    CONSTRAINT `fk_notification_recipient_notification`
        FOREIGN KEY (`notification_id`) REFERENCES `community_notification` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notification_recipient_user`
        FOREIGN KEY (`recipient_user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_notification_recipient_status`
        CHECK (`status` IN ('UNREAD', 'READ', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='站内通知收件箱';

CREATE TABLE IF NOT EXISTS `file_object` (
    `id` BIGINT UNSIGNED NOT NULL,
    `storage_provider` VARCHAR(24) NOT NULL DEFAULT 'LOCAL',
    `bucket_name` VARCHAR(120) NULL,
    `object_key` VARCHAR(500) NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(120) NOT NULL,
    `size_bytes` BIGINT UNSIGNED NOT NULL,
    `sha256` CHAR(64) NOT NULL,
    `status` VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    `created_by_user_id` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_object_location` (`storage_provider`, `bucket_name`, `object_key`),
    KEY `idx_file_object_hash` (`sha256`, `size_bytes`),
    KEY `idx_file_object_status_created` (`status`, `created_at`),
    CONSTRAINT `fk_file_object_creator`
        FOREIGN KEY (`created_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_file_object_status`
        CHECK (`status` IN ('UPLOADING', 'ACTIVE', 'QUARANTINED', 'DELETED', 'PURGED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='社区文件对象元数据';

CREATE TABLE IF NOT EXISTS `community_file_reference` (
    `id` BIGINT UNSIGNED NOT NULL,
    `file_id` BIGINT UNSIGNED NOT NULL,
    `owner_user_id` BIGINT UNSIGNED NULL,
    `target_type` VARCHAR(32) NOT NULL,
    `target_id` BIGINT UNSIGNED NOT NULL,
    `usage_type` VARCHAR(32) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_reference_target`
        (`file_id`, `target_type`, `target_id`, `usage_type`),
    KEY `idx_file_reference_target` (`target_type`, `target_id`, `deleted_at`),
    KEY `idx_file_reference_owner` (`owner_user_id`, `created_at`),
    CONSTRAINT `fk_file_reference_file`
        FOREIGN KEY (`file_id`) REFERENCES `file_object` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_file_reference_owner`
        FOREIGN KEY (`owner_user_id`) REFERENCES `community_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='社区文件业务引用';

-- 执行后验证：应返回 15。
SELECT COUNT(*) AS `m1_core_table_count`
FROM `information_schema`.`tables`
WHERE `table_schema` = DATABASE()
  AND `table_name` IN (
      'community_user',
      'community_user_login_account',
      'community_user_preference',
      'blog',
      'blog_setting',
      'blog_category',
      'article',
      'article_version',
      'platform_tag',
      'article_tag',
      'content_review_task',
      'community_notification',
      'community_notification_recipient',
      'file_object',
      'community_file_reference'
  );
