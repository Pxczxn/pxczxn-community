SELECT CASE WHEN COUNT(*)=1 THEN 'PASS' ELSE 'FAIL' END AS result
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='community_user_login_account' AND COLUMN_NAME='force_password_change';
