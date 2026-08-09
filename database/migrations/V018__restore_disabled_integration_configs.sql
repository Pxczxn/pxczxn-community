SET NAMES utf8mb4;

-- Retain extension points without restoring any historical credentials.
-- These groups stay disabled until an operator explicitly configures them.
INSERT INTO `sys_config_group` (
    `id`, `group_code`, `group_name`, `group_icon`, `config_value`,
    `sort`, `status`, `remark`, `create_time`, `update_time`
) VALUES
    (7, 'sms', '短信配置', NULL, JSON_OBJECT('enabled', FALSE), 7, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (11, 'thirdParty', '第三方配置', NULL, JSON_OBJECT('enabled', FALSE), 11, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (12, 'payment', '支付配置', NULL, JSON_OBJECT('enabled', FALSE), 12, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (15, 'wechatMiniProgram', '小程序配置', NULL, JSON_OBJECT('enabled', FALSE), 14, 0, '微信小程序登录配置', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (16, 'wechatMp', '公众号配置', NULL, JSON_OBJECT('enabled', FALSE), 15, 0, '微信公众号配置', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    `group_name` = VALUES(`group_name`),
    `group_icon` = VALUES(`group_icon`),
    `config_value` = JSON_OBJECT('enabled', FALSE),
    `sort` = VALUES(`sort`),
    `status` = 0,
    `remark` = VALUES(`remark`),
    `update_time` = CURRENT_TIMESTAMP;
