-- 账号管理菜单 9199：V050 在审核中心下创建，后续账号治理重构将其归入“账号治理”目录并命名“账号管理”。
SELECT CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE id = 9199
  AND parent_id = 9205
  AND name = '账号管理'
  AND path = '/community/account-enforcements'
  AND permission = 'community:account:list'
  AND deleted = 0;
