SELECT CASE WHEN COUNT(*) = 2 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE (`id` = 9191 AND `name` = '社区处置')
   OR (`id` = 9199 AND `name` = '账号操作中心');
