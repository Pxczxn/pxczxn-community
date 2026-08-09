SET NAMES utf8mb4;

-- 账号处置菜单已并入账号管理（逻辑删除）
SELECT CASE WHEN NOT EXISTS (
  SELECT 1 FROM `sys_menu` WHERE `id` = 9191 AND `deleted` = 0
) THEN 1 ELSE 0 END AS sanction_menu_merged_into_account_management;

-- 账号管理菜单保留且命名正确
SELECT CASE WHEN EXISTS (
  SELECT 1 FROM `sys_menu` WHERE `id` = 9199 AND `name` = '账号管理' AND `deleted` = 0
) THEN 1 ELSE 0 END AS account_management_menu_ready;
