SET NAMES utf8mb4;

SELECT
    CASE WHEN COUNT(*) = 1
          AND JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.provider')) = 'local'
          AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.minioAccessKey')), '') = ''
          AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.minioSecretKey')), '') = ''
          AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.aliyunAccessKey')), '') = ''
          AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.aliyunSecretKey')), '') = ''
          AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.tencentSecretId')), '') = ''
          AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.tencentSecretKey')), '') = ''
          AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.rustfsAccessKey')), '') = ''
          AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.rustfsSecretKey')), '') = ''
         THEN 'PASS' ELSE 'FAIL' END AS `storage_scaffold_credentials_redacted`
FROM `sys_config_group`
WHERE `group_code` = 'storage';
