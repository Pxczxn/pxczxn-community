/*
 * Repair menu IDs 9140-9142, which were reused by the sanction and editorial
 * migrations after they had already been assigned to the taxonomy section.
 *
 * The only implemented series operation in the admin console is review. Move
 * that working page back below "标签与系列" and retire the two stale entries
 * that have no route or component.
 */
SET NAMES utf8mb4;

INSERT INTO `sys_menu` (
  `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`, `icon`,
  `sort`, `visible`, `status`, `is_frame`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`
) VALUES
  (9140, 0, '标签与系列', 1, '/taxonomy', NULL, NULL, 'PricetagOutline',
   7, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
  (9190, 9140, '系列审核', 2, '/community/series', '/community/series/index', 'community:series:review', 'AlbumsOutline',
   2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0)
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
  `deleted` = 0,
  `update_time` = CURRENT_TIMESTAMP,
  `update_by` = 1;

/* 9141 and 9142 are stale records left by the historical ID collisions. */
UPDATE `sys_menu`
SET `visible` = 0,
    `status` = 0,
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = 1
WHERE `id` IN (9141, 9142)
  AND `deleted` = 0;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, required_menu.menu_id
FROM `sys_role` r
CROSS JOIN (
  SELECT 9140 AS menu_id
  UNION ALL SELECT 9190
) required_menu
WHERE r.code = 'admin'
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existing
    WHERE existing.role_id = r.id
      AND existing.menu_id = required_menu.menu_id
  );
