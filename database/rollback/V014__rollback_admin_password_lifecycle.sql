/*
 * Destructive rollback for V014.
 * Back up sys_user before execution. Password lifecycle metadata is discarded.
 */

SET NAMES utf8mb4;

SET @temporary_password_issued_at_column_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'temporary_password_issued_at'
);
SET @drop_temporary_password_issued_at_sql := IF(
    @temporary_password_issued_at_column_count > 0,
    'ALTER TABLE `sys_user` DROP COLUMN `temporary_password_issued_at`',
    'SELECT ''temporary_password_issued_at already absent'''
);
PREPARE drop_temporary_password_issued_at_stmt
    FROM @drop_temporary_password_issued_at_sql;
EXECUTE drop_temporary_password_issued_at_stmt;
DEALLOCATE PREPARE drop_temporary_password_issued_at_stmt;

SET @password_changed_at_column_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'password_changed_at'
);
SET @drop_password_changed_at_sql := IF(
    @password_changed_at_column_count > 0,
    'ALTER TABLE `sys_user` DROP COLUMN `password_changed_at`',
    'SELECT ''password_changed_at already absent'''
);
PREPARE drop_password_changed_at_stmt FROM @drop_password_changed_at_sql;
EXECUTE drop_password_changed_at_stmt;
DEALLOCATE PREPARE drop_password_changed_at_stmt;

SET @must_change_password_column_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'must_change_password'
);
SET @drop_must_change_password_sql := IF(
    @must_change_password_column_count > 0,
    'ALTER TABLE `sys_user` DROP COLUMN `must_change_password`',
    'SELECT ''must_change_password already absent'''
);
PREPARE drop_must_change_password_stmt FROM @drop_must_change_password_sql;
EXECUTE drop_must_change_password_stmt;
DEALLOCATE PREPARE drop_must_change_password_stmt;
