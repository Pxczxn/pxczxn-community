SET NAMES utf8mb4;

UPDATE `sys_role`
SET `remark`='可查询账号、执行最长 7 天的临时冻结、提交高风险账号操作申请并完成初审',
    `update_time`=CURRENT_TIMESTAMP
WHERE `code`='community_admin' AND `deleted`=0;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 9203
FROM `sys_role` r
WHERE r.code='community_admin' AND r.deleted=0
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` existing
    WHERE existing.role_id=r.id AND existing.menu_id=9203
  );
