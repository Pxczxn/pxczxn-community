/*
 * Version: V009
 * Purpose: M2 评论范围约束与评论治理审计
 * Depends on: V008
 * Locking: CREATE TABLE/ALTER TABLE 获取元数据锁；在维护窗口执行
 * Historical data migration: 既有 ALL_LOGGED_IN/FOLLOWERS_ONLY/DISABLED 均合法
 * Backup required: 已部署环境执行前必须完成数据库备份
 * Repeatable: 是，建表和约束操作均检查既有结构
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS `community_comment_moderation_event` (
    `id` BIGINT UNSIGNED NOT NULL,
    `comment_id` BIGINT UNSIGNED NOT NULL,
    `action` VARCHAR(32) NOT NULL,
    `actor_type` VARCHAR(16) NOT NULL,
    `actor_user_id` BIGINT UNSIGNED NULL,
    `actor_admin_id` BIGINT NULL,
    `previous_status` VARCHAR(32) NULL,
    `new_status` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(500) NULL,
    `metadata_json` JSON NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_comment_moderation_comment_created`
        (`comment_id`, `created_at`, `id`),
    KEY `idx_comment_moderation_action_created`
        (`action`, `created_at`, `id`),
    CONSTRAINT `fk_comment_moderation_comment`
        FOREIGN KEY (`comment_id`)
        REFERENCES `community_comment` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_comment_moderation_actor_user`
        FOREIGN KEY (`actor_user_id`)
        REFERENCES `community_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_comment_moderation_action`
        CHECK (`action` IN (
            'AUTO_PUBLISHED', 'AUTO_REVIEW_QUEUED', 'USER_DELETED',
            'AUTHOR_HIDDEN', 'BLOG_HIDDEN', 'PLATFORM_TAKEN_DOWN',
            'PLATFORM_RESTORED'
        )),
    CONSTRAINT `chk_comment_moderation_actor_type`
        CHECK (`actor_type` IN ('USER', 'ADMIN', 'SYSTEM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='评论提交、删除、隐藏和平台治理的不可变事件';

SET @comment_scope_constraint_count := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'blog_setting'
      AND CONSTRAINT_NAME = 'chk_blog_setting_comment_scope'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @comment_scope_constraint_sql := IF(
    @comment_scope_constraint_count = 0,
    'ALTER TABLE `blog_setting`
       ADD CONSTRAINT `chk_blog_setting_comment_scope`
       CHECK (`comment_scope` IN (
         ''ALL_LOGGED_IN'', ''FOLLOWERS_ONLY'', ''MUTUAL_ONLY'',
         ''BLOGGER_FOLLOWING'', ''TEAM_FOLLOWERS'', ''TEAM_MEMBERS'',
         ''DISABLED''
       ))',
    'SELECT ''comment scope constraint already exists'''
);
PREPARE comment_scope_constraint_stmt
    FROM @comment_scope_constraint_sql;
EXECUTE comment_scope_constraint_stmt;
DEALLOCATE PREPARE comment_scope_constraint_stmt;
