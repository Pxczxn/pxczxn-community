SELECT CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE id = 9191
  AND name = '账号管理'
  AND permission = 'community:sanction:list';
