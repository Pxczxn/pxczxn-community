SET NAMES utf8mb4;

INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`) VALUES
 (9194,9191,'查询账号措施',3,NULL,NULL,'community:account:list',NULL,10,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9195,9191,'执行临时冻结',3,NULL,NULL,'community:account:freeze',NULL,11,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9196,9191,'提交重大措施申请',3,NULL,NULL,'community:account:apply',NULL,12,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9197,9191,'审核重大措施',3,NULL,NULL,'community:account:approve',NULL,13,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9198,9191,'执行重大措施',3,NULL,NULL,'community:account:execute',NULL,14,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`),`permission`=VALUES(`permission`),`deleted`=0,`update_time`=CURRENT_TIMESTAMP;

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.id, m.menu_id FROM `sys_role` r CROSS JOIN (
 SELECT 9194 menu_id UNION ALL SELECT 9195 UNION ALL SELECT 9196 UNION ALL SELECT 9197 UNION ALL SELECT 9198
) m
WHERE r.code='admin' AND r.deleted=0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` e WHERE e.role_id=r.id AND e.menu_id=m.menu_id);
