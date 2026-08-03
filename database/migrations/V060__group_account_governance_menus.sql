SET NAMES utf8mb4;

INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`)
VALUES (9205,0,'账号治理',1,'/account-governance',NULL,NULL,'ShieldOutline',5,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0)
ON DUPLICATE KEY UPDATE
  `parent_id`=VALUES(`parent_id`), `name`=VALUES(`name`), `type`=VALUES(`type`), `path`=VALUES(`path`),
  `component`=VALUES(`component`), `permission`=VALUES(`permission`), `icon`=VALUES(`icon`), `sort`=VALUES(`sort`),
  `visible`=VALUES(`visible`), `status`=VALUES(`status`), `deleted`=0, `update_time`=CURRENT_TIMESTAMP;

UPDATE `sys_menu` SET `parent_id`=9205, `sort`=1, `update_time`=CURRENT_TIMESTAMP WHERE `id`=9194;
UPDATE `sys_menu` SET `parent_id`=9205, `sort`=2, `update_time`=CURRENT_TIMESTAMP WHERE `id`=9191;
UPDATE `sys_menu` SET `parent_id`=9205, `sort`=3, `update_time`=CURRENT_TIMESTAMP WHERE `id`=9199;

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT DISTINCT `role_id`, 9205
FROM `sys_role_menu` source
WHERE source.`menu_id` IN (9191,9194,9199)
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` existing
    WHERE existing.`role_id`=source.`role_id` AND existing.`menu_id`=9205
  );
