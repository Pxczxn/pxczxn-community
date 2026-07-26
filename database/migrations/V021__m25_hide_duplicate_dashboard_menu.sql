/*
 * Version: V021
 * Purpose: Keep one visible operations-overview entry in the dynamic sidebar.
 * Depends on: V020 M2.5 information architecture.
 * Repeatable: Yes.
 */

SET NAMES utf8mb4;

/* The layout owns the fixed “运营总览” entry; retain its permission menu but hide its duplicate DB item. */
UPDATE `sys_menu`
SET `visible` = 0,
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = 1
WHERE `id` = 9030
  AND `path` = '/community/dashboard'
  AND `deleted` = 0;
