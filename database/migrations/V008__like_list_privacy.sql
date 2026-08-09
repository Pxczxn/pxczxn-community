/*
 * Version: V008
 * Purpose: M2 喜欢列表公开范围
 * Depends on: V007
 * Locking: ALTER TABLE 获取元数据锁；在维护窗口执行
 * Historical data migration: 既有用户统一采用 PRIVATE 默认值
 * Backup required: 已部署环境执行前必须完成数据库备份
 * Repeatable: 是，执行前检查字段与约束是否已经存在
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

SET @likes_visibility_column_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_user_preference'
      AND COLUMN_NAME = 'likes_visibility'
);
SET @likes_visibility_column_sql := IF(
    @likes_visibility_column_count = 0,
    'ALTER TABLE `community_user_preference`
       ADD COLUMN `likes_visibility` VARCHAR(24) NOT NULL DEFAULT ''PRIVATE''
       AFTER `content_language`',
    'SELECT ''likes_visibility already exists'''
);
PREPARE likes_visibility_column_stmt
    FROM @likes_visibility_column_sql;
EXECUTE likes_visibility_column_stmt;
DEALLOCATE PREPARE likes_visibility_column_stmt;

SET @likes_visibility_constraint_count := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_user_preference'
      AND CONSTRAINT_NAME = 'chk_user_preference_likes_visibility'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @likes_visibility_constraint_sql := IF(
    @likes_visibility_constraint_count = 0,
    'ALTER TABLE `community_user_preference`
       ADD CONSTRAINT `chk_user_preference_likes_visibility`
       CHECK (`likes_visibility` IN (
         ''PRIVATE'', ''PUBLIC'', ''FOLLOWERS_ONLY'', ''MUTUAL_ONLY''
       ))',
    'SELECT ''likes visibility constraint already exists'''
);
PREPARE likes_visibility_constraint_stmt
    FROM @likes_visibility_constraint_sql;
EXECUTE likes_visibility_constraint_stmt;
DEALLOCATE PREPARE likes_visibility_constraint_stmt;
