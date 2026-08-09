/*
 * Version: V004
 * Purpose: Register article review workbench routes and actions in Mars Admin RBAC.
 * Depends on: V002 community root menu and Mars Admin sys_menu/sys_role/sys_role_menu.
 * Repeatable: Yes. Fixed menu IDs plus guarded role mappings make this idempotent.
 */

SET NAMES utf8mb4;

INSERT INTO `sys_menu` (
    `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`,
    `icon`, `sort`, `visible`, `status`, `is_frame`,
    `create_by`, `update_by`, `deleted`
) VALUES
    (9020, 9000, '文章审核', 2, '/community/reviews',
     '/community/reviews/index', 'community:review:list',
     'ShieldCheckmarkOutline', 2, 1, 1, 0, 1, 1, 0),
    (9021, 9020, '审核详情', 3, NULL, NULL,
     'community:review:query', NULL, 1, 1, 1, 0, 1, 1, 0),
    (9022, 9020, '领取审核', 3, NULL, NULL,
     'community:review:claim', NULL, 2, 1, 1, 0, 1, 1, 0),
    (9023, 9020, '审核通过', 3, NULL, NULL,
     'community:review:approve', NULL, 3, 1, 1, 0, 1, 1, 0),
    (9024, 9020, '要求修改', 3, NULL, NULL,
     'community:review:revision', NULL, 4, 1, 1, 0, 1, 1, 0),
    (9025, 9020, '审核驳回', 3, NULL, NULL,
     'community:review:reject', NULL, 5, 1, 1, 0, 1, 1, 0),
    (9026, 9020, '文章审核域权限', 3, NULL, NULL,
     'community:article:review', NULL, 6, 0, 1, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `type` = VALUES(`type`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `permission` = VALUES(`permission`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `is_frame` = VALUES(`is_frame`),
    `update_by` = VALUES(`update_by`),
    `deleted` = VALUES(`deleted`);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT `r`.`id`, `menu_ids`.`menu_id`
FROM `sys_role` AS `r`
CROSS JOIN (
    SELECT 9000 AS `menu_id`
    UNION ALL SELECT 9020
    UNION ALL SELECT 9021
    UNION ALL SELECT 9022
    UNION ALL SELECT 9023
    UNION ALL SELECT 9024
    UNION ALL SELECT 9025
    UNION ALL SELECT 9026
) AS `menu_ids`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `r`.`id`
        AND `existing`.`menu_id` = `menu_ids`.`menu_id`
  );
