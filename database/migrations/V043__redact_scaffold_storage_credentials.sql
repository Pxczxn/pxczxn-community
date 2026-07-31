SET NAMES utf8mb4;

/*
 * Remove scaffold/sample object-storage credentials from the database-backed
 * configuration. Production credentials are supplied only by controlled
 * operations tooling after this migration has been applied.
 */
UPDATE `sys_config_group`
SET `config_value` = JSON_SET(
    JSON_REMOVE(
        `config_value`,
        '$.minioAccessKey', '$.minioSecretKey',
        '$.aliyunAccessKey', '$.aliyunSecretKey',
        '$.tencentSecretId', '$.tencentSecretKey',
        '$.rustfsAccessKey', '$.rustfsSecretKey'
    ),
    '$.provider', 'local',
    '$.domain', '',
    '$.minioEndpoint', '',
    '$.minioBucket', '',
    '$.aliyunEndpoint', '',
    '$.aliyunBucket', '',
    '$.tencentBucket', '',
    '$.tencentRegion', '',
    '$.rustfsEndpoint', '',
    '$.rustfsBucket', ''
)
WHERE `group_code` = 'storage';
