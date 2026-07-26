/*
 * Version: V014
 * Purpose: Store the administrator password lifecycle state required for
 *          one-time passwords and mandatory first-login password changes.
 * Depends on: V013 and the existing sys_user table.
 * Existing users: remain usable and are not forced to change their password.
 * Repeatable: yes; every schema change is guarded through information_schema.
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

SET @must_change_password_column_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'must_change_password'
);
SET @add_must_change_password_sql := IF(
    @must_change_password_column_count = 0,
    'ALTER TABLE `sys_user`
       ADD COLUMN `must_change_password` TINYINT NOT NULL DEFAULT 0
       COMMENT ''Whether a password change is required (0-no 1-yes)''
       AFTER `password`',
    'SELECT ''must_change_password already exists'''
);
PREPARE add_must_change_password_stmt FROM @add_must_change_password_sql;
EXECUTE add_must_change_password_stmt;
DEALLOCATE PREPARE add_must_change_password_stmt;

SET @password_changed_at_column_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'password_changed_at'
);
SET @add_password_changed_at_sql := IF(
    @password_changed_at_column_count = 0,
    'ALTER TABLE `sys_user`
       ADD COLUMN `password_changed_at` DATETIME NULL
       COMMENT ''Time of the latest user-selected password change''
       AFTER `must_change_password`',
    'SELECT ''password_changed_at already exists'''
);
PREPARE add_password_changed_at_stmt FROM @add_password_changed_at_sql;
EXECUTE add_password_changed_at_stmt;
DEALLOCATE PREPARE add_password_changed_at_stmt;

SET @temporary_password_issued_at_column_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'temporary_password_issued_at'
);
SET @add_temporary_password_issued_at_sql := IF(
    @temporary_password_issued_at_column_count = 0,
    'ALTER TABLE `sys_user`
       ADD COLUMN `temporary_password_issued_at` DATETIME NULL
       COMMENT ''Time the current one-time password was issued''
       AFTER `password_changed_at`',
    'SELECT ''temporary_password_issued_at already exists'''
);
PREPARE add_temporary_password_issued_at_stmt
    FROM @add_temporary_password_issued_at_sql;
EXECUTE add_temporary_password_issued_at_stmt;
DEALLOCATE PREPARE add_temporary_password_issued_at_stmt;

