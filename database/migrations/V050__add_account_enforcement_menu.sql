SET NAMES utf8mb4;

INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`) VALUES
  (9199,9110,'账号强制措施',2,'/community/account-enforcements','/community/account-enforcements/index','community:account:list','ShieldOutline',6,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0)
ON DUPLICATE KEY UPDATE
  `parent_id`=VALUES(`parent_id`),`name`=VALUES(`name`),`type`=VALUES(`type`),`path`=VALUES(`path`),`component`=VALUES(`component`),`permission`=VALUES(`permission`),`icon`=VALUES(`icon`),`sort`=VALUES(`sort`),`visible`=VALUES(`visible`),`status`=VALUES(`status`),`deleted`=0,`update_time`=CURRENT_TIMESTAMP;

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.id,9199 FROM `sys_role` r
WHERE r.code='admin' AND r.deleted=0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` e WHERE e.role_id=r.id AND e.menu_id=9199);
