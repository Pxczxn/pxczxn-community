/*
 * Rollback script for V024__m3_team_application_review.sql
 * Purpose: Remove team application review menus, permissions, and slug constraint.
 * Usage: Run to reverse V024 migration if needed.
 * WARNING: This does not delete team_application data (created by V023).
 */

SET NAMES utf8mb4;

-- Restore the V023 non-unique slug lookup index after removing V024's active-slug constraint.
ALTER TABLE `team_application`
    DROP INDEX IF EXISTS `uk_team_slug_active`,
    DROP COLUMN IF EXISTS `is_slug_active`,
    ADD KEY IF NOT EXISTS `idx_application_slug` (`team_slug`);

-- Remove admin role menu mappings for team application review
DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (9111, 9112, 9113);

-- Soft delete team application review menus
UPDATE `sys_menu`
SET `deleted` = 1, `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` IN (9111, 9112, 9113)
  AND `deleted` = 0;

SELECT 'R024 rollback complete' AS result;
