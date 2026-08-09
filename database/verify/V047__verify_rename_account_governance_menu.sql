-- 账号处置菜单重命名：V047 将 9191 命名为“账号管理”；
-- 后续 V070 将其并入“账号管理”目录并逻辑删除，两种最终状态均视为通过。
SELECT CASE WHEN
  EXISTS (
    SELECT 1
    FROM sys_menu
    WHERE id = 9191
      AND name = '账号管理'
      AND permission = 'community:sanction:list'
      AND deleted = 0
  )
  OR NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE id = 9191 AND deleted = 0
  )
THEN 'PASS' ELSE 'FAIL' END AS result;
