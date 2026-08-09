SET NAMES utf8mb4;

DELETE `rm`
FROM `sys_role_menu` `rm`
JOIN `sys_menu` `m` ON `m`.`id` = `rm`.`menu_id`
WHERE `m`.`id` IN (139, 140, 154, 162, 170, 279)
   OR `m`.`parent_id` IN (140, 154, 162, 170)
   OR `m`.`path` IN (
       '/message/chat',
       '/monitor/server-manager',
       '/test',
       '/test/test',
       '/tool/gen',
       '/system/customer',
       'system/student'
   )
   OR `m`.`component` IN (
       '/message/chat/index',
       '/monitor/server-manager/index',
       '/test/test/index',
       '/tool/gen/index',
       '/system/customer/index',
       'system/student/index'
   )
   OR `m`.`permission` LIKE 'sys:chat:%'
   OR `m`.`permission` LIKE 'monitor:server:%'
   OR `m`.`permission` LIKE 'tool:gen:%'
   OR `m`.`permission` LIKE 'system:customer:%'
   OR `m`.`permission` LIKE 'system:student:%';

DELETE FROM `sys_menu`
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

DELETE FROM `sys_config_group`
WHERE `group_code` IN (
    'sms',
    'payment',
    'thirdParty',
    'wechatMiniProgram',
    'wechatMp'
);

DELETE FROM `sys_chat_group_message`;
DELETE FROM `sys_chat_group_member`;
DELETE FROM `sys_chat_group`;
DELETE FROM `sys_chat_message`;
DELETE FROM `sys_server`;
DELETE FROM `sys_sms_log`;
DELETE FROM `gen_table_column`;
DELETE FROM `gen_table`;
DELETE FROM `student`;
