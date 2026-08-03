SELECT CASE WHEN COUNT(*) = 5 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE id IN (9194,9195,9196,9197,9198)
  AND permission IN ('community:account:list','community:account:freeze','community:account:apply','community:account:approve','community:account:execute')
  AND deleted = 0;
