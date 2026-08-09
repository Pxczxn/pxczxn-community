SELECT
    JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.siteName'))
        AS `site_name`,
    JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.siteDescription'))
        AS `site_description`,
    JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.copyright'))
        AS `copyright`
FROM `sys_config_group`
WHERE `group_code` = 'system'
  AND `status` = 1;

SELECT
    COUNT(*) AS `governance_menu_count`,
    SUM(`name` LIKE '%?%') AS `invalid_menu_name_count`
FROM `sys_menu`
WHERE `id` BETWEEN 9070 AND 9091
  AND `deleted` = 0;
