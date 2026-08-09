SET NAMES utf8mb4;

UPDATE `sys_config_group`
SET `status` = 0,
    `config_value` = JSON_OBJECT('enabled', FALSE),
    `update_time` = CURRENT_TIMESTAMP
WHERE `group_code` IN (
    'sms',
    'payment',
    'thirdParty',
    'wechatMiniProgram',
    'wechatMp'
);

UPDATE `sys_config_group`
SET `config_value` = JSON_SET(
        JSON_REMOVE(`config_value`, '$.wechat_work'),
        '$.dingtalk.tokenId', '',
        '$.dingtalk.signName', '',
        '$.feishu.tokenId', '',
        '$.feishu.signName', ''
    ),
    `update_time` = CURRENT_TIMESTAMP
WHERE `group_code` = 'push'
  AND JSON_VALID(`config_value`);
