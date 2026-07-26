SET NAMES utf8mb4;

SELECT IF(
    COUNT(*) = 1 AND MAX(`visible`) = 0,
    'PASS',
    'FAIL'
) AS `duplicate_dashboard_menu_hidden`
FROM `sys_menu`
WHERE `id` = 9030
  AND `path` = '/community/dashboard'
  AND `deleted` = 0;
