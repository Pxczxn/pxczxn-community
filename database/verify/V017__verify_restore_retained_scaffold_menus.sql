SET NAMES utf8mb4;

SELECT IF(
      (SELECT COUNT(*) FROM `sys_menu` WHERE `id` = 45 AND `deleted` = 0 AND `status` = 1) = 1
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id` = 139 AND `deleted` = 0 AND `status` = 1) = 1,
    'PASS',
    'FAIL'
) AS `retained_active_menus_restored`;

SELECT IF(
      (SELECT COUNT(*) FROM `sys_menu` WHERE `id` IN (140, 141, 154, 162, 170, 327) AND (`visible` <> 0 OR `status` <> 0)) = 0
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id` IN (140, 141, 154, 162, 170, 327) AND `deleted` = 0) = 6,
    'PASS',
    'FAIL'
) AS `future_scaffold_menus_retained_but_inactive`;

SELECT IF(
      (SELECT COUNT(*) FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 139) = 1
  AND (SELECT COUNT(*) FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 139) = 1,
    'PASS',
    'FAIL'
) AS `chat_role_menu_links_restored`;

SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `role_menu_links_consistent`
FROM `sys_role_menu` `rm`
LEFT JOIN `sys_menu` `m` ON `m`.`id` = `rm`.`menu_id`
WHERE `m`.`id` IS NULL;
