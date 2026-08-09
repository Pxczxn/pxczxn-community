/*
 * Version: V002
 * Purpose: Register community tag management in the Mars Admin menu/RBAC model.
 * Depends on: Mars Admin sys_menu/sys_role/sys_role_menu and V001 platform_tag.
 * Repeatable: Yes. Fixed menu IDs plus guarded role mappings make this idempotent.
 */

SET NAMES utf8mb4;

INSERT INTO `sys_menu` (
    `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`,
    `icon`, `sort`, `visible`, `status`, `is_frame`,
    `create_by`, `update_by`, `deleted`
) VALUES
    (9000, 0, '社区管理', 1, '/community', NULL, NULL,
     'PeopleOutline', 5, 1, 1, 0, 1, 1, 0),
    (9010, 9000, '平台标签', 2, '/community/tags', '/community/tags/index',
     'community:tag:list', 'PricetagsOutline', 1, 1, 1, 0, 1, 1, 0),
    (9011, 9010, '查询标签', 3, NULL, NULL,
     'community:tag:list', NULL, 1, 1, 1, 0, 1, 1, 0),
    (9012, 9010, '新增标签', 3, NULL, NULL,
     'community:tag:add', NULL, 2, 1, 1, 0, 1, 1, 0),
    (9013, 9010, '编辑标签', 3, NULL, NULL,
     'community:tag:edit', NULL, 3, 1, 1, 0, 1, 1, 0),
    (9014, 9010, '删除标签', 3, NULL, NULL,
     'community:tag:delete', NULL, 4, 1, 1, 0, 1, 1, 0)
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
    UNION ALL SELECT 9010
    UNION ALL SELECT 9011
    UNION ALL SELECT 9012
    UNION ALL SELECT 9013
    UNION ALL SELECT 9014
) AS `menu_ids`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `r`.`id`
        AND `existing`.`menu_id` = `menu_ids`.`menu_id`
  );
