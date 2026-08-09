/* Repair M4 sanction permissions after historical community menu ID collisions. */
SET NAMES utf8mb4;

INSERT INTO `sys_menu` (
  `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`, `icon`,
  `sort`, `visible`, `status`, `is_frame`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`
) VALUES
  (9191, 9110, '处罚体系', 2, '/community/sanctions', '/community/sanctions/index', 'community:sanction:list', 'HammerOutline', 5, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
  (9192, 9191, '查询处罚', 3, NULL, NULL, 'community:sanction:list', NULL, 1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
  (9193, 9191, '执行处罚', 3, NULL, NULL, 'community:sanction:handle', NULL, 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0)
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
  `deleted` = 0,
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, required_menu.menu_id
FROM `sys_role` r
CROSS JOIN (
  SELECT 9191 AS menu_id
  UNION ALL SELECT 9192
  UNION ALL SELECT 9193
) required_menu
WHERE r.code = 'admin' AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existing
    WHERE existing.role_id = r.id AND existing.menu_id = required_menu.menu_id
  );
