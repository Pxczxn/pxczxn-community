SET NAMES utf8mb4;

-- 团队管理菜单 9130-9131 保留且归属正确（V044 的修复成果）
SELECT CASE WHEN
  EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `id` = 9131
      AND `parent_id` = 9130
      AND `type` = 2
      AND `path` = '/community/teams'
      AND `component` = '/community/teams/index'
      AND `permission` = 'community:team:list'
      AND `visible` = 1
      AND `status` = 1
      AND `deleted` = 0
  )
  AND EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `id` = 9194
      AND `type` = 2
      AND `path` = '/community/appeals'
      AND `component` = '/community/appeals/index'
      AND `permission` = 'community:appeal:list'
      AND `visible` = 1
      AND `status` = 1
      AND `deleted` = 0
  )
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id` IN (9195, 9196) AND `parent_id` = 9194 AND `type` = 3 AND `deleted` = 0) = 2
  AND EXISTS (
    SELECT 1
    FROM `sys_role_menu` rm
    JOIN `sys_role` r ON r.`id` = rm.`role_id`
    WHERE r.`code` = 'admin'
      AND r.`deleted` = 0
      AND rm.`menu_id` = 9131
  )
THEN 'PASS' ELSE 'FAIL' END AS `team_and_appeal_menu_collision_repaired`;
