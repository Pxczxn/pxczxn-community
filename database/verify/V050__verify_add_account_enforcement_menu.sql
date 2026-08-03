SELECT CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE id=9199 AND parent_id=9110 AND name='账号强制措施'
  AND path='/community/account-enforcements' AND permission='community:account:list'
  AND deleted=0;
