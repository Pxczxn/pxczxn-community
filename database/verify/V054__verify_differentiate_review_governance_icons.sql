SELECT CASE WHEN COUNT(*) = 3 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE (`id` = 9020 AND `icon` = 'DocumentTextOutline')
   OR (`id` = 9120 AND `icon` = 'HammerOutline')
   OR (`id` = 9199 AND `icon` = 'KeyOutline');
