SELECT CASE WHEN `icon` = 'CheckmarkCircleOutline' THEN 'PASS' ELSE 'FAIL' END AS result
FROM `sys_menu`
WHERE `id` = 9110 AND `deleted` = 0;
