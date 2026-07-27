/*
 * Version: V024
 * Purpose: M3-T002 Team application review - menu and permissions for platform team approval workflow.
 *          Adds a generated active-slug marker and conditional unique constraint to prevent
 *          concurrent slug conflicts across PENDING and APPROVED applications.
 * Depends on: V023 (team_application table) and V020 (menu structure).
 * This is a versioned migration and must be executed once by the migration runner.
 *
 * Adds "团队申请审核" submenu under "审核中心" (9110) with community:team:review permission.
 * Platform operators with this permission can approve/reject team creation applications.
 */

SET NAMES utf8mb4;

-- Add a conditional unique constraint for active (PENDING/APPROVED) applications.
-- MySQL unique indexes allow multiple NULL values, so REJECTED/CANCELLED rows remain reusable.
-- A plain (team_slug, status) unique key is insufficient because it permits one PENDING and
-- one APPROVED row with the same slug at the same time.
ALTER TABLE `team_application`
    DROP INDEX IF EXISTS `idx_application_slug`,
    ADD COLUMN IF NOT EXISTS `is_slug_active` TINYINT
        AS (IF(`status` IN ('PENDING', 'APPROVED'), 1, NULL)) STORED
        COMMENT 'Generated: 1 for slug-reserving application statuses, NULL otherwise',
    ADD UNIQUE KEY IF NOT EXISTS `uk_team_slug_active` (`team_slug`, `is_slug_active`) USING BTREE
        COMMENT 'Prevents duplicate slug across active applications';

-- Insert team application review menu under "审核中心" (9110)
INSERT INTO `sys_menu` (
    `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`,
    `icon`, `sort`, `visible`, `status`, `is_frame`,
    `create_time`, `update_time`, `create_by`, `update_by`, `deleted`
) VALUES
    (9111, 9110, '团队申请审核', 2, '/community/team-applications', '/community/team-applications/index',
     'community:team:review', 'PeopleOutline', 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9112, 9111, '查询申请', 3, NULL, NULL,
     'community:team:review', NULL, 1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9113, 9111, '审批申请', 3, NULL, NULL,
     'community:team:review', NULL, 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0)
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
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = VALUES(`update_by`),
    `deleted` = VALUES(`deleted`);

-- Grant team application review permission to admin role
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT `r`.`id`, `menu_ids`.`menu_id`
FROM `sys_role` AS `r`
CROSS JOIN (
    SELECT 9111 AS `menu_id`
    UNION ALL SELECT 9112
    UNION ALL SELECT 9113
) AS `menu_ids`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `r`.`id`
        AND `existing`.`menu_id` = `menu_ids`.`menu_id`
  );
