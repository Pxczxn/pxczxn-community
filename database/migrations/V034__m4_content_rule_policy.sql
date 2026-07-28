/* M4-T005: policy metadata for keyword moderation rules. */
SET NAMES utf8mb4;

ALTER TABLE `content_keyword_rule`
    ADD COLUMN `content_scopes` VARCHAR(64) NOT NULL DEFAULT 'ARTICLE,COMMENT,MOMENT' COMMENT 'Comma-separated ARTICLE,COMMENT,MOMENT or ALL',
    ADD COLUMN `risk_level` VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW,MEDIUM,HIGH,CRITICAL',
    ADD COLUMN `hit_action` VARCHAR(24) NOT NULL DEFAULT 'WARN' COMMENT 'BLOCK,MANUAL_REVIEW,WARN';

UPDATE `content_keyword_rule`
SET `risk_level` = CASE `severity`
        WHEN 'BLOCK' THEN 'CRITICAL'
        WHEN 'REVIEW' THEN 'HIGH'
        ELSE 'MEDIUM'
    END,
    `hit_action` = CASE `severity`
        WHEN 'BLOCK' THEN 'BLOCK'
        WHEN 'REVIEW' THEN 'MANUAL_REVIEW'
        ELSE 'WARN'
    END;

ALTER TABLE `content_keyword_rule`
    ADD CONSTRAINT `chk_keyword_rule_risk_level` CHECK (`risk_level` IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    ADD CONSTRAINT `chk_keyword_rule_hit_action` CHECK (`hit_action` IN ('BLOCK','MANUAL_REVIEW','WARN'));

INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`) VALUES
 (9150,9110,'内容规则',2,'/community/content-rules','/community/content-rules/index','community:content-rule:list','FilterOutline',23,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9151,9150,'查询规则',3,NULL,NULL,'community:content-rule:list',NULL,1,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9152,9150,'维护规则',3,NULL,NULL,'community:content-rule:manage',NULL,2,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0)
ON DUPLICATE KEY UPDATE `permission`=VALUES(`permission`),`path`=VALUES(`path`),`component`=VALUES(`component`),`update_time`=CURRENT_TIMESTAMP;
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) SELECT r.id,v.menu_id FROM `sys_role` r CROSS JOIN (SELECT 9150 menu_id UNION ALL SELECT 9151 UNION ALL SELECT 9152) v WHERE r.code='admin' AND r.deleted=0 AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` e WHERE e.role_id=r.id AND e.menu_id=v.menu_id);
