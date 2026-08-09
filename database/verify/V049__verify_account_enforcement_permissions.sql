-- 账号强制措施菜单：V049 建立 9194-9198，后续 V051 将措施按钮迁移到 9200-9204，
-- 此处验证最终生效的账号措施权限菜单。
SELECT CASE WHEN COUNT(*) = 5 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE id IN (9200, 9201, 9202, 9203, 9204)
  AND parent_id = 9199
  AND permission IN ('community:account:list','community:account:freeze','community:account:apply','community:account:approve','community:account:execute')
  AND deleted = 0;
