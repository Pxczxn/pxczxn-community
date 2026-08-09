SET NAMES utf8mb4;

SELECT IF(
    COUNT(*) = 3,
    'PASS',
    'FAIL'
) AS `admin_password_lifecycle_columns`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'sys_user'
  AND COLUMN_NAME IN (
      'must_change_password',
      'password_changed_at',
      'temporary_password_issued_at'
  );

SELECT IF(
    COUNT(*) = 1,
    'PASS',
    'FAIL'
) AS `must_change_password_definition`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'sys_user'
  AND COLUMN_NAME = 'must_change_password'
  AND IS_NULLABLE = 'NO'
  AND COLUMN_DEFAULT = '0'
  AND DATA_TYPE = 'tinyint';

SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `existing_password_lifecycle_rows`
FROM `sys_user`
WHERE `must_change_password` IS NULL
   OR `must_change_password` NOT IN (0, 1);

