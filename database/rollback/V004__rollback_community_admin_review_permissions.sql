/*
 * Removes only the V004 article review menu and role-menu records.
 * It does not delete review tasks, articles, versions, or notifications.
 */

SET NAMES utf8mb4;

DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (9020, 9021, 9022, 9023, 9024, 9025, 9026);

DELETE FROM `sys_menu`
WHERE `id` IN (9021, 9022, 9023, 9024, 9025, 9026, 9020);
