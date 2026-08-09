SET NAMES utf8mb4;

-- 账号处置菜单已重命名为“账号管理”
SELECT CASE WHEN EXISTS (
  SELECT 1 FROM `sys_menu`
  WHERE `id` = 9199
    AND `path` = '/community/account-enforcements'
    AND `name` = '账号管理'
    AND `deleted` = 0
) THEN 1 ELSE 0 END AS account_enforcement_menu_renamed;
