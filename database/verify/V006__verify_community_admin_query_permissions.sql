SET NAMES utf8mb4;

SELECT
    CASE WHEN COUNT(*) = 9 THEN 'PASS' ELSE 'FAIL' END
        AS `community_query_menu_count_check`,
    COUNT(*) AS `actual_menu_count`
FROM `sys_menu`
WHERE `id` IN (9030, 9031, 9040, 9041, 9050, 9051, 9060, 9061, 9062)
  AND `deleted` = 0;

SELECT
    CASE WHEN COUNT(DISTINCT `permission`) = 5 THEN 'PASS' ELSE 'FAIL' END
        AS `community_query_permission_count_check`,
    COUNT(DISTINCT `permission`) AS `actual_permission_count`
FROM `sys_menu`
WHERE `permission` IN (
    'community:dashboard:view',
    'community:user:list',
    'community:blog:list',
    'community:article:list',
    'community:article:query'
)
  AND `deleted` = 0;

SELECT
    CASE WHEN COUNT(*) = 10 THEN 'PASS' ELSE 'FAIL' END
        AS `admin_community_query_grant_check`,
    COUNT(*) AS `actual_admin_grants`
FROM `sys_role_menu` AS `rm`
JOIN `sys_role` AS `r` ON `r`.`id` = `rm`.`role_id`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND `rm`.`menu_id` IN (
      9000, 9030, 9031, 9040, 9041,
      9050, 9051, 9060, 9061, 9062
  );

SELECT
    CASE WHEN COUNT(*) = 6 THEN 'PASS' ELSE 'FAIL' END
        AS `community_page_order_check`,
    COUNT(*) AS `actual_ordered_pages`
FROM `sys_menu`
WHERE `id` IN (9030, 9040, 9050, 9060, 9020, 9010)
  AND `sort` = CASE `id`
      WHEN 9030 THEN 1
      WHEN 9040 THEN 2
      WHEN 9050 THEN 3
      WHEN 9060 THEN 4
      WHEN 9020 THEN 5
      WHEN 9010 THEN 6
  END
  AND `deleted` = 0;

SELECT
    CASE
        WHEN JSON_UNQUOTE(JSON_EXTRACT(`config_value`, '$.siteName'))
             = '星语社区运营中心'
        THEN 'PASS'
        ELSE 'FAIL'
    END AS `community_admin_brand_check`
FROM `sys_config_group`
WHERE `group_code` = 'system';
