/*
 * Removes only V006 community query menus and role-menu records.
 * Existing review and tag menus remain in place.
 */

SET NAMES utf8mb4;

DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (
    9030, 9031, 9040, 9041, 9050, 9051, 9060, 9061, 9062
);

DELETE FROM `sys_menu`
WHERE `id` IN (
    9031, 9030, 9041, 9040, 9051, 9050, 9062, 9061, 9060
);

UPDATE `sys_menu`
SET `sort` = 2, `update_by` = 1
WHERE `id` = 9020 AND `deleted` = 0;

UPDATE `sys_menu`
SET `sort` = 1, `update_by` = 1
WHERE `id` = 9010 AND `deleted` = 0;
