/*
 * Version: V011
 * Purpose: M2 站内通知分类、聚合、重要等级与稳定去重
 * Depends on: V010
 * Locking: ALTER TABLE/CREATE INDEX 获取元数据锁；在维护窗口执行
 * Historical data migration: 既有审核通知回填 REVIEW，其余回填 SYSTEM
 * Backup required: 已部署环境执行前必须完成数据库备份
 * Repeatable: 是，字段、索引和约束操作均检查既有结构
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

SET @notification_category_column := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND COLUMN_NAME = 'category'
);
SET @notification_category_sql := IF(
    @notification_category_column = 0,
    'ALTER TABLE `community_notification`
       ADD COLUMN `category` VARCHAR(16) NOT NULL DEFAULT ''SYSTEM''
       AFTER `notification_type`',
    'SELECT ''notification category already exists'''
);
PREPARE notification_category_stmt FROM @notification_category_sql;
EXECUTE notification_category_stmt;
DEALLOCATE PREPARE notification_category_stmt;

SET @notification_importance_column := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND COLUMN_NAME = 'importance'
);
SET @notification_importance_sql := IF(
    @notification_importance_column = 0,
    'ALTER TABLE `community_notification`
       ADD COLUMN `importance` VARCHAR(16) NOT NULL DEFAULT ''NORMAL''
       AFTER `category`',
    'SELECT ''notification importance already exists'''
);
PREPARE notification_importance_stmt FROM @notification_importance_sql;
EXECUTE notification_importance_stmt;
DEALLOCATE PREPARE notification_importance_stmt;

SET @notification_aggregate_column := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND COLUMN_NAME = 'aggregate_count'
);
SET @notification_aggregate_sql := IF(
    @notification_aggregate_column = 0,
    'ALTER TABLE `community_notification`
       ADD COLUMN `aggregate_count` INT UNSIGNED NOT NULL DEFAULT 1
       AFTER `payload_json`',
    'SELECT ''notification aggregate count already exists'''
);
PREPARE notification_aggregate_stmt FROM @notification_aggregate_sql;
EXECUTE notification_aggregate_stmt;
DEALLOCATE PREPARE notification_aggregate_stmt;

SET @notification_activity_column := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND COLUMN_NAME = 'last_activity_at'
);
SET @notification_activity_sql := IF(
    @notification_activity_column = 0,
    'ALTER TABLE `community_notification`
       ADD COLUMN `last_activity_at` DATETIME(3) NOT NULL
         DEFAULT CURRENT_TIMESTAMP(3)
       AFTER `aggregate_count`',
    'SELECT ''notification activity time already exists'''
);
PREPARE notification_activity_stmt FROM @notification_activity_sql;
EXECUTE notification_activity_stmt;
DEALLOCATE PREPARE notification_activity_stmt;

UPDATE `community_notification`
SET `category` = CASE
        WHEN `notification_type` = 'REVIEW' THEN 'REVIEW'
        ELSE `category`
    END,
    `importance` = COALESCE(NULLIF(`importance`, ''), 'NORMAL'),
    `aggregate_count` = GREATEST(COALESCE(`aggregate_count`, 1), 1),
    `last_activity_at` = COALESCE(`last_activity_at`, `created_at`);

/*
 * V001 只有普通去重索引。唯一键是并发幂等的最终防线。若历史环境存在重复键，
 * 保留最早一条的键，其余通知仍保留但清空键，避免迁移删除用户收件箱记录。
 */
UPDATE `community_notification` duplicate_row
JOIN (
    SELECT `deduplication_key`, MIN(`id`) AS `keep_id`
    FROM `community_notification`
    WHERE `deduplication_key` IS NOT NULL
    GROUP BY `deduplication_key`
    HAVING COUNT(*) > 1
) duplicate_key
  ON duplicate_key.`deduplication_key` =
     duplicate_row.`deduplication_key`
SET duplicate_row.`deduplication_key` = NULL
WHERE duplicate_row.`id` <> duplicate_key.`keep_id`;

SET @old_notification_dedup_index := (
    SELECT COUNT(DISTINCT INDEX_NAME)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND INDEX_NAME = 'idx_notification_deduplication'
);
SET @old_notification_dedup_sql := IF(
    @old_notification_dedup_index > 0,
    'DROP INDEX `idx_notification_deduplication`
       ON `community_notification`',
    'SELECT ''old notification deduplication index absent'''
);
PREPARE old_notification_dedup_stmt FROM @old_notification_dedup_sql;
EXECUTE old_notification_dedup_stmt;
DEALLOCATE PREPARE old_notification_dedup_stmt;

SET @notification_dedup_unique := (
    SELECT COUNT(DISTINCT INDEX_NAME)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND INDEX_NAME = 'uk_notification_deduplication'
      AND NON_UNIQUE = 0
);
SET @notification_dedup_unique_sql := IF(
    @notification_dedup_unique = 0,
    'CREATE UNIQUE INDEX `uk_notification_deduplication`
       ON `community_notification` (`deduplication_key`)',
    'SELECT ''notification unique deduplication index already exists'''
);
PREPARE notification_dedup_unique_stmt
    FROM @notification_dedup_unique_sql;
EXECUTE notification_dedup_unique_stmt;
DEALLOCATE PREPARE notification_dedup_unique_stmt;

SET @notification_inbox_index := (
    SELECT COUNT(DISTINCT INDEX_NAME)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND INDEX_NAME = 'idx_notification_category_activity'
);
SET @notification_inbox_index_sql := IF(
    @notification_inbox_index = 0,
    'CREATE INDEX `idx_notification_category_activity`
       ON `community_notification`
          (`category`, `last_activity_at`, `id`)',
    'SELECT ''notification category index already exists'''
);
PREPARE notification_inbox_index_stmt FROM @notification_inbox_index_sql;
EXECUTE notification_inbox_index_stmt;
DEALLOCATE PREPARE notification_inbox_index_stmt;

SET @notification_category_check := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND CONSTRAINT_NAME = 'chk_notification_category'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @notification_category_check_sql := IF(
    @notification_category_check = 0,
    'ALTER TABLE `community_notification`
       ADD CONSTRAINT `chk_notification_category`
       CHECK (`category` IN (
         ''INTERACTION'', ''FOLLOW'', ''COMMENT'', ''COAUTHOR'',
         ''SUBMISSION'', ''TEAM'', ''REVIEW'', ''SYSTEM''
       ))',
    'SELECT ''notification category constraint already exists'''
);
PREPARE notification_category_check_stmt
    FROM @notification_category_check_sql;
EXECUTE notification_category_check_stmt;
DEALLOCATE PREPARE notification_category_check_stmt;

SET @notification_importance_check := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND CONSTRAINT_NAME = 'chk_notification_importance'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @notification_importance_check_sql := IF(
    @notification_importance_check = 0,
    'ALTER TABLE `community_notification`
       ADD CONSTRAINT `chk_notification_importance`
       CHECK (`importance` IN (''NORMAL'', ''HIGH''))',
    'SELECT ''notification importance constraint already exists'''
);
PREPARE notification_importance_check_stmt
    FROM @notification_importance_check_sql;
EXECUTE notification_importance_check_stmt;
DEALLOCATE PREPARE notification_importance_check_stmt;

SET @notification_aggregate_check := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_notification'
      AND CONSTRAINT_NAME = 'chk_notification_aggregate_count'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @notification_aggregate_check_sql := IF(
    @notification_aggregate_check = 0,
    'ALTER TABLE `community_notification`
       ADD CONSTRAINT `chk_notification_aggregate_count`
       CHECK (`aggregate_count` >= 1)',
    'SELECT ''notification aggregate constraint already exists'''
);
PREPARE notification_aggregate_check_stmt
    FROM @notification_aggregate_check_sql;
EXECUTE notification_aggregate_check_stmt;
DEALLOCATE PREPARE notification_aggregate_check_stmt;
