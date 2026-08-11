/*
 * Version: V077
 * Purpose: Persist community client preference settings without expanding account profile columns
 * Depends on: V076
 * Locking: ALTER TABLE obtains a metadata lock; execute in a maintenance window for production
 * Historical data migration: existing rows remain NULL and are treated as empty settings
 * Backup required: complete a database backup before production execution
 * Repeatable: yes, checks whether the column already exists
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

SET @settings_json_column_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_user_preference'
      AND COLUMN_NAME = 'settings_json'
);
SET @settings_json_column_sql := IF(
    @settings_json_column_count = 0,
    'ALTER TABLE `community_user_preference`
       ADD COLUMN `settings_json` JSON NULL COMMENT ''non-relational community client preferences''
       AFTER `likes_visibility`',
    'SELECT ''settings_json already exists'''
);
PREPARE settings_json_column_stmt FROM @settings_json_column_sql;
EXECUTE settings_json_column_stmt;
DEALLOCATE PREPARE settings_json_column_stmt;
