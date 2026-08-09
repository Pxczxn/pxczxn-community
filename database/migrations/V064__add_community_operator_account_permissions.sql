SET NAMES utf8mb4;

INSERT INTO `sys_role` (`name`, `code`, `sort`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`, `data_scope`)
VALUES ('社区管理员', 'community_admin', 20, 1, '可查询账号、执行最长 7 天的临时冻结，并提交高风险账号处置申请', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `status`=1, `remark`=VALUES(`remark`), `deleted`=0, `update_time`=CURRENT_TIMESTAMP;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.menu_id
FROM `sys_role` r
CROSS JOIN (SELECT 9205 AS menu_id UNION ALL SELECT 9199 UNION ALL SELECT 9200 UNION ALL SELECT 9201 UNION ALL SELECT 9202) m
WHERE r.code='community_admin' AND r.deleted=0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` existing WHERE existing.role_id=r.id AND existing.menu_id=m.menu_id);
