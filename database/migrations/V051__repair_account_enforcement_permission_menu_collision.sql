SET NAMES utf8mb4;

/* Restore appeal center nodes accidentally reused by V049. */
INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`) VALUES
 (9194,9110,'申诉中心',2,'/community/appeals','/community/appeals/index','community:appeal:list','ShieldCheck',4,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9195,9194,'查询申诉',3,NULL,NULL,'community:appeal:list',NULL,1,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9196,9194,'复核申诉',3,NULL,NULL,'community:appeal:handle',NULL,2,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0)
ON DUPLICATE KEY UPDATE `parent_id`=VALUES(`parent_id`),`name`=VALUES(`name`),`type`=VALUES(`type`),`path`=VALUES(`path`),`component`=VALUES(`component`),`permission`=VALUES(`permission`),`icon`=VALUES(`icon`),`sort`=VALUES(`sort`),`visible`=VALUES(`visible`),`status`=VALUES(`status`),`deleted`=0,`update_time`=CURRENT_TIMESTAMP;

INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`) VALUES
 (9200,9199,'查询账号措施',3,NULL,NULL,'community:account:list',NULL,1,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9201,9199,'执行临时冻结',3,NULL,NULL,'community:account:freeze',NULL,2,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9202,9199,'提交重大措施申请',3,NULL,NULL,'community:account:apply',NULL,3,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9203,9199,'审核重大措施',3,NULL,NULL,'community:account:approve',NULL,4,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9204,9199,'执行重大措施',3,NULL,NULL,'community:account:execute',NULL,5,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0)
ON DUPLICATE KEY UPDATE `parent_id`=VALUES(`parent_id`),`name`=VALUES(`name`),`type`=VALUES(`type`),`path`=VALUES(`path`),`component`=VALUES(`component`),`permission`=VALUES(`permission`),`sort`=VALUES(`sort`),`visible`=VALUES(`visible`),`status`=VALUES(`status`),`deleted`=0,`update_time`=CURRENT_TIMESTAMP;

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.id, m.menu_id FROM `sys_role` r CROSS JOIN (
 SELECT 9200 menu_id UNION ALL SELECT 9201 UNION ALL SELECT 9202 UNION ALL SELECT 9203 UNION ALL SELECT 9204
) m WHERE r.code='admin' AND r.deleted=0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` e WHERE e.role_id=r.id AND e.menu_id=m.menu_id);
