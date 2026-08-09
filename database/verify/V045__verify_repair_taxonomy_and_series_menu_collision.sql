SET NAMES utf8mb4;

SELECT CASE WHEN
  EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `id` = 9140
      AND `parent_id` = 0
      AND `type` = 1
      AND `path` = '/taxonomy'
      AND `component` IS NULL
      AND `visible` = 1
      AND `status` = 1
      AND `deleted` = 0
  )
  AND EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `id` = 9190
      AND `parent_id` = 9140
      AND `type` = 2
      AND `path` = '/community/series'
      AND `component` = '/community/series/index'
      AND `permission` = 'community:series:review'
      AND `visible` = 1
      AND `status` = 1
      AND `deleted` = 0
  )
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id` IN (9141, 9142) AND `visible` = 0 AND `status` = 0 AND `deleted` = 0) = 2
  AND EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    JOIN `sys_role` r ON r.`id` = rm.`role_id`
    WHERE r.`code` = 'admin'
      AND r.`deleted` = 0
      AND rm.`menu_id` = 9190
  )
THEN 'PASS' ELSE 'FAIL' END AS `taxonomy_and_series_menu_collision_repaired`;
