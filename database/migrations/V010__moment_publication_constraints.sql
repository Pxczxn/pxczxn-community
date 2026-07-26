/*
 * Version: V010
 * Purpose: M2 动态发布内容形状约束与公开流索引
 * Depends on: V009
 * Locking: ALTER TABLE/CREATE INDEX 获取元数据锁；在维护窗口执行
 * Historical data migration: 不改写历史数据，执行前由约束验证现有行
 * Backup required: 已部署环境执行前必须完成数据库备份
 * Repeatable: 是，索引和约束操作均检查既有结构
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

SET @moment_feed_index_count := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_moment'
      AND INDEX_NAME = 'idx_community_moment_public_feed'
);
SET @moment_feed_index_sql := IF(
    @moment_feed_index_count = 0,
    'CREATE INDEX `idx_community_moment_public_feed`
       ON `community_moment`
          (`status`, `visibility`, `created_at`, `id`)',
    'SELECT ''moment public feed index already exists'''
);
PREPARE moment_feed_index_stmt FROM @moment_feed_index_sql;
EXECUTE moment_feed_index_stmt;
DEALLOCATE PREPARE moment_feed_index_stmt;

/*
 * article_id 与 repost_moment_id 都参与 ON DELETE SET NULL 外键。MySQL 8.0
 * 禁止这类列同时出现在 CHECK 中，因此两列的互斥、类型匹配与必填规则由
 * MomentService 在同一发布事务中校验，数据库继续使用既有外键保证引用合法。
 */

SET @moment_link_constraint_count := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_moment'
      AND CONSTRAINT_NAME = 'chk_community_moment_link_required'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @moment_link_constraint_sql := IF(
    @moment_link_constraint_count = 0,
    'ALTER TABLE `community_moment`
       ADD CONSTRAINT `chk_community_moment_link_required`
       CHECK (
         `moment_type` NOT IN (''LINK'', ''VIDEO_LINK'')
         OR (`link_url` IS NOT NULL AND CHAR_LENGTH(TRIM(`link_url`)) > 0)
       )',
    'SELECT ''moment link constraint already exists'''
);
PREPARE moment_link_constraint_stmt FROM @moment_link_constraint_sql;
EXECUTE moment_link_constraint_stmt;
DEALLOCATE PREPARE moment_link_constraint_stmt;

SET @moment_quote_constraint_count := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_moment'
      AND CONSTRAINT_NAME = 'chk_community_moment_quote_text'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @moment_quote_constraint_sql := IF(
    @moment_quote_constraint_count = 0,
    'ALTER TABLE `community_moment`
       ADD CONSTRAINT `chk_community_moment_quote_text`
       CHECK (
         `moment_type` <> ''QUOTE''
         OR (
           `text_content` IS NOT NULL
           AND CHAR_LENGTH(TRIM(`text_content`)) > 0
         )
       )',
    'SELECT ''moment quote constraint already exists'''
);
PREPARE moment_quote_constraint_stmt FROM @moment_quote_constraint_sql;
EXECUTE moment_quote_constraint_stmt;
DEALLOCATE PREPARE moment_quote_constraint_stmt;

SET @moment_repost_constraint_count := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_moment'
      AND CONSTRAINT_NAME = 'chk_community_moment_repost_text'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @moment_repost_constraint_sql := IF(
    @moment_repost_constraint_count = 0,
    'ALTER TABLE `community_moment`
       ADD CONSTRAINT `chk_community_moment_repost_text`
       CHECK (
         `moment_type` <> ''REPOST''
         OR `text_content` IS NULL
         OR CHAR_LENGTH(TRIM(`text_content`)) = 0
       )',
    'SELECT ''moment repost constraint already exists'''
);
PREPARE moment_repost_constraint_stmt FROM @moment_repost_constraint_sql;
EXECUTE moment_repost_constraint_stmt;
DEALLOCATE PREPARE moment_repost_constraint_stmt;
