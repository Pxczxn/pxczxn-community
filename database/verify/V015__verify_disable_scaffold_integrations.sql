SET NAMES utf8mb4;

SELECT IF(
    COUNT(*) = 5
      AND SUM(`status` = 0) = 5
      AND SUM(
          JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.enabled')) = 'false'
      ) = 5,
    'PASS',
    'FAIL'
) AS `disabled_scaffold_integrations`
FROM `sys_config_group`
WHERE `group_code` IN (
    'sms',
    'payment',
    'thirdParty',
    'wechatMiniProgram',
    'wechatMp'
);

SELECT IF(
    COUNT(*) = 1
      AND JSON_CONTAINS_PATH(`config_value`, 'one', '$.wechat_work') = 0
      AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.dingtalk.tokenId')), '') = ''
      AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.dingtalk.signName')), '') = ''
      AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.feishu.tokenId')), '') = ''
      AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.feishu.signName')), '') = '',
    'PASS',
    'FAIL'
) AS `redacted_external_push_credentials`
FROM `sys_config_group`
WHERE `group_code` = 'push';
