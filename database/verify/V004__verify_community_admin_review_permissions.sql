SET NAMES utf8mb4;

SELECT
    CASE WHEN COUNT(*) = 7 THEN 'PASS' ELSE 'FAIL' END
        AS `review_menu_count_check`,
    COUNT(*) AS `actual_review_menu_count`
FROM `sys_menu`
WHERE `id` IN (9020, 9021, 9022, 9023, 9024, 9025, 9026)
  AND `deleted` = 0;

SELECT
    CASE WHEN COUNT(DISTINCT `permission`) = 7 THEN 'PASS' ELSE 'FAIL' END
        AS `review_permission_count_check`,
    COUNT(DISTINCT `permission`) AS `actual_review_permission_count`
FROM `sys_menu`
WHERE `permission` IN (
    'community:review:list',
    'community:review:query',
    'community:review:claim',
    'community:review:approve',
    'community:review:revision',
    'community:review:reject',
    'community:article:review'
)
  AND `deleted` = 0;

SELECT
    CASE WHEN COUNT(*) = 8 THEN 'PASS' ELSE 'FAIL' END
        AS `admin_review_grant_check`,
    COUNT(*) AS `actual_admin_review_grants`
FROM `sys_role_menu` AS `rm`
JOIN `sys_role` AS `r` ON `r`.`id` = `rm`.`role_id`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND `rm`.`menu_id` IN (
      9000, 9020, 9021, 9022, 9023, 9024, 9025, 9026
  );
