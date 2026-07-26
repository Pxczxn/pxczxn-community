SET NAMES utf8mb4;

SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `scaffold_menus_removed`
FROM `sys_menu`
WHERE `id` IN (139, 140, 154, 162, 170, 279)
   OR `parent_id` IN (140, 154, 162, 170)
   OR `path` IN (
       '/message/chat',
       '/monitor/server-manager',
       '/test',
       '/test/test',
       '/tool/gen',
       '/system/customer',
       'system/student'
   )
   OR `component` IN (
       '/message/chat/index',
       '/monitor/server-manager/index',
       '/test/test/index',
       '/tool/gen/index',
       '/system/customer/index',
       'system/student/index'
   )
   OR `permission` LIKE 'sys:chat:%'
   OR `permission` LIKE 'monitor:server:%'
   OR `permission` LIKE 'tool:gen:%'
   OR `permission` LIKE 'system:customer:%'
   OR `permission` LIKE 'system:student:%';

SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `role_menu_links_consistent`
FROM `sys_role_menu` `rm`
LEFT JOIN `sys_menu` `m` ON `m`.`id` = `rm`.`menu_id`
WHERE `m`.`id` IS NULL;

SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `scaffold_config_groups_removed`
FROM `sys_config_group`
WHERE `group_code` IN (
    'sms',
    'payment',
    'thirdParty',
    'wechatMiniProgram',
    'wechatMp'
);

SELECT IF(
      (SELECT COUNT(*) FROM `sys_chat_group_message`) = 0
  AND (SELECT COUNT(*) FROM `sys_chat_group_member`) = 0
  AND (SELECT COUNT(*) FROM `sys_chat_group`) = 0
  AND (SELECT COUNT(*) FROM `sys_chat_message`) = 0
  AND (SELECT COUNT(*) FROM `sys_server`) = 0
  AND (SELECT COUNT(*) FROM `sys_sms_log`) = 0
  AND (SELECT COUNT(*) FROM `gen_table_column`) = 0
  AND (SELECT COUNT(*) FROM `gen_table`) = 0
  AND (SELECT COUNT(*) FROM `student`) = 0,
    'PASS',
    'FAIL'
) AS `scaffold_sample_rows_removed`;

SELECT IF(
      (SELECT COUNT(*) FROM `sys_menu` WHERE `id` = 1 AND `deleted` = 0) = 1
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id` = 126 AND `deleted` = 0) = 1
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id` = 9000 AND `deleted` = 0) = 1,
    'PASS',
    'FAIL'
) AS `retained_platform_menus_available`;
