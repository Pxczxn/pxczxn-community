SELECT CASE WHEN `icon` = 'GitBranchOutline' THEN 'PASS' ELSE 'FAIL' END AS result
FROM `sys_menu`
WHERE `id` = 9205 AND `deleted` = 0;
