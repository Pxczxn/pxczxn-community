SELECT CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE `id` = 9120 AND `icon` = 'ShieldOutline';
