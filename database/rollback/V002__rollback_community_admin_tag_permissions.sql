/*
 * Removes only the V002 menu and role-menu records.
 * It does not delete any platform_tag business data.
 */

SET NAMES utf8mb4;

DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (9000, 9010, 9011, 9012, 9013, 9014);

DELETE FROM `sys_menu`
WHERE `id` IN (9011, 9012, 9013, 9014, 9010, 9000);
