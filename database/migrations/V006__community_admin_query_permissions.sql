/*
 * Version: V006
 * Purpose: Register the community operations dashboard and read-only query pages.
 * Depends on: V002 community root menu and Mars Admin sys_menu/sys_role/sys_role_menu.
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
    (9030, 9000, '社区工作台', 2, '/community/dashboard',
     '/community/dashboard/index', 'community:dashboard:view',
     'GridOutline', 1, 1, 1, 0, 1, 1, 0),
    (9031, 9030, '查看社区工作台', 3, NULL, NULL,
     'community:dashboard:view', NULL, 1, 1, 1, 0, 1, 1, 0),
    (9040, 9000, '社区用户', 2, '/community/users',
     '/community/users/index', 'community:user:list',
     'PersonOutline', 2, 1, 1, 0, 1, 1, 0),
    (9041, 9040, '查询社区用户', 3, NULL, NULL,
     'community:user:list', NULL, 1, 1, 1, 0, 1, 1, 0),
    (9050, 9000, '博客管理', 2, '/community/blogs',
     '/community/blogs/index', 'community:blog:list',
     'AppsOutline', 3, 1, 1, 0, 1, 1, 0),
    (9051, 9050, '查询博客', 3, NULL, NULL,
     'community:blog:list', NULL, 1, 1, 1, 0, 1, 1, 0),
    (9060, 9000, '文章管理', 2, '/community/articles',
     '/community/articles/index', 'community:article:list',
     'DocumentTextOutline', 4, 1, 1, 0, 1, 1, 0),
    (9061, 9060, '查询文章', 3, NULL, NULL,
     'community:article:list', NULL, 1, 1, 1, 0, 1, 1, 0),
    (9062, 9060, '查看文章详情', 3, NULL, NULL,
     'community:article:query', NULL, 2, 1, 1, 0, 1, 1, 0)
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

UPDATE `sys_menu`
SET `sort` = 5, `icon` = 'ShieldOutline', `update_by` = 1
WHERE `id` = 9020 AND `deleted` = 0;

UPDATE `sys_menu`
SET `sort` = 6, `icon` = 'PricetagOutline', `update_by` = 1
WHERE `id` = 9010 AND `deleted` = 0;

UPDATE `sys_config_group`
SET `config_value` = JSON_SET(
        `config_value`,
        '$.siteName', '星语社区运营中心',
        '$.siteDescription', '博客社区运营与治理中心',
        '$.copyright', '版权所有 © 星语社区 2026'
    )
WHERE `group_code` = 'system'
  AND JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.siteName'))
      IN ('Mars Admin', 'Mars-Admin');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT `r`.`id`, `menu_ids`.`menu_id`
FROM `sys_role` AS `r`
CROSS JOIN (
    SELECT 9000 AS `menu_id`
    UNION ALL SELECT 9030
    UNION ALL SELECT 9031
    UNION ALL SELECT 9040
    UNION ALL SELECT 9041
    UNION ALL SELECT 9050
    UNION ALL SELECT 9051
    UNION ALL SELECT 9060
    UNION ALL SELECT 9061
    UNION ALL SELECT 9062
) AS `menu_ids`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `r`.`id`
        AND `existing`.`menu_id` = `menu_ids`.`menu_id`
  );
