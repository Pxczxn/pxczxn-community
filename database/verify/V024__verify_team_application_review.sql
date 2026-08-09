/*
 * Verification script for V024__m3_team_application_review.sql
 * Purpose: Verify that team application review menus and permissions exist.
 * Usage: Run after V024 migration to confirm successful execution.
 */

SET NAMES utf8mb4;

-- Verify that the conditional slug-reservation column and index exist.
SELECT 'Checking active team slug constraint...' AS step;

SELECT COUNT(*) AS active_slug_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_application'
  AND COLUMN_NAME = 'is_slug_active'
  AND EXTRA LIKE '%STORED GENERATED%';
-- Expected: 1

SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_application'
  AND INDEX_NAME = 'uk_team_slug_active'
ORDER BY SEQ_IN_INDEX;
-- Expected two rows: team_slug (1), is_slug_active (2), both NON_UNIQUE = 0

-- Verify team application review menus exist
SELECT 'Checking team application review menus...' AS step;

SELECT COUNT(*) AS menu_count
FROM `sys_menu`
WHERE `id` IN (9111, 9112, 9113)
  AND `deleted` = 0;
-- Expected: 3

-- Verify menu structure
SELECT `id`, `parent_id`, `name`, `permission`
FROM `sys_menu`
WHERE `id` IN (9111, 9112, 9113)
  AND `deleted` = 0
ORDER BY `id`;
-- Expected:
-- 9111, 9110, '团队申请审核', 'community:team:review'
-- 9112, 9111, '查询申请', 'community:team:review'
-- 9113, 9111, '审批申请', 'community:team:review'

-- Verify admin role has access to team application review
SELECT 'Checking admin role permissions...' AS step;

SELECT COUNT(*) AS role_menu_count
FROM `sys_role_menu` AS `rm`
JOIN `sys_role` AS `r` ON `rm`.`role_id` = `r`.`id`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND `rm`.`menu_id` IN (9111, 9112, 9113);
-- Expected: 3

SELECT 'V024 verification complete' AS result;
