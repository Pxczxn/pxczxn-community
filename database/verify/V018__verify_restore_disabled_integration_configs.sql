SET NAMES utf8mb4;

SELECT IF(
    COUNT(*) = 5
      AND SUM(`status` = 0) = 5
      AND SUM(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.enabled')) = 'false') = 5
      AND SUM(JSON_LENGTH(`config_value`) = 1) = 5,
    'PASS',
    'FAIL'
) AS `retained_integrations_are_disabled_and_redacted`
FROM `sys_config_group`
WHERE `group_code` IN (
    'sms',
    'payment',
    'thirdParty',
    'wechatMiniProgram',
    'wechatMp'
);
