SELECT CASE WHEN COUNT(*) = 3 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE (id = 9191 AND name = '账号处置' AND permission = 'community:sanction:list')
   OR (id = 9192 AND name = '查看社区处置' AND permission = 'community:sanction:list')
   OR (id = 9193 AND name = '执行社区处置' AND permission = 'community:sanction:handle');
