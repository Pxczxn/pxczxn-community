SET NAMES utf8mb4;

SELECT
    CASE WHEN COUNT(*) = 6 THEN 'PASS' ELSE 'FAIL' END AS `menu_count_check`,
    COUNT(*) AS `actual_menu_count`
FROM `sys_menu`
WHERE `id` IN (9000, 9010, 9011, 9012, 9013, 9014)
  AND `deleted` = 0;

SELECT
    CASE WHEN COUNT(DISTINCT `permission`) = 4 THEN 'PASS' ELSE 'FAIL' END
        AS `permission_count_check`,
    COUNT(DISTINCT `permission`) AS `actual_permission_count`
FROM `sys_menu`
WHERE `permission` IN (
    'community:tag:list',
    'community:tag:add',
    'community:tag:edit',
    'community:tag:delete'
)
  AND `deleted` = 0;

SELECT
    `r`.`code` AS `role_code`,
    COUNT(*) AS `community_menu_grants`
FROM `sys_role_menu` AS `rm`
JOIN `sys_role` AS `r` ON `r`.`id` = `rm`.`role_id`
WHERE `r`.`code` = 'admin'
  AND `rm`.`menu_id` IN (9000, 9010, 9011, 9012, 9013, 9014)
GROUP BY `r`.`code`;
