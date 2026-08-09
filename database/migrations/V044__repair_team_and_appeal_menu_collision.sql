/*
 * Repair menu IDs 9130-9132, which were first assigned to team management in
 * V020 and then incorrectly reused by the appeal-center migration V032.
 *
 * Keep the established team IDs and assign the appeal center new, unused IDs.
 */
SET NAMES utf8mb4;

INSERT INTO `sys_menu` (
  `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`, `icon`,
  `sort`, `visible`, `status`, `is_frame`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`
) VALUES
  (9130, 0, '团队管理', 1, '/teams', NULL, NULL, 'PeopleOutline',
   6, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
  (9131, 9130, '团队管理', 2, '/community/teams', '/community/teams/index', 'community:team:list', 'PeopleOutline',
   1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
  (9194, 9110, '申诉中心', 2, '/community/appeals', '/community/appeals/index', 'community:appeal:list', 'ShieldCheck',
   21, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
  (9195, 9194, '查询申诉', 3, NULL, NULL, 'community:appeal:list', NULL,
   1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
  (9196, 9194, '复核申诉', 3, NULL, NULL, 'community:appeal:handle', NULL,
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

/* 9132 was the old appeal action ID; retain history but do not render it. */
UPDATE `sys_menu`
SET `visible` = 0,
    `status` = 0,
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = 1
WHERE `id` = 9132 AND `deleted` = 0;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, required_menu.menu_id
FROM `sys_role` r
CROSS JOIN (
  SELECT 9130 AS menu_id
  UNION ALL SELECT 9131
  UNION ALL SELECT 9194
  UNION ALL SELECT 9195
  UNION ALL SELECT 9196
) required_menu
WHERE r.code = 'admin'
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existing
    WHERE existing.role_id = r.id
      AND existing.menu_id = required_menu.menu_id
  );
