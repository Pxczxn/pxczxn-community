/* Repair the M3 team-series review permission after historical menu ID collisions. */
SET NAMES utf8mb4;

INSERT INTO `sys_menu` (
  `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`, `icon`,
  `sort`, `visible`, `status`, `is_frame`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`
) VALUES (
  9190, 9110, '系列审核', 2, '/community/series', '/community/series/index', 'community:series:review', 'AlbumsOutline',
  4, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0
)
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
SELECT `id`, 9190
FROM `sys_role`
WHERE `code` = 'admin' AND `deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu`
    WHERE `role_id` = `sys_role`.`id` AND `menu_id` = 9190
  );
