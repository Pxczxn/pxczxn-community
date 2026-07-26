SET NAMES utf8mb4;

SELECT IF(
    COUNT(*) = 4,
    'PASS',
    'FAIL'
) AS `retained_capabilities_nested_under_system_settings`
FROM `sys_menu`
WHERE `id` IN (31, 36, 126, 161)
  AND `parent_id` = 1
  AND `deleted` = 0;

SELECT IF(
    COUNT(*) = 10,
    'PASS',
    'FAIL'
) AS `visible_primary_community_navigation_only`
FROM `sys_menu`
WHERE `parent_id` = 0
  AND `visible` = 1
  AND `status` = 1
  AND `deleted` = 0
  AND `id` IN (1, 9000, 9100, 9110, 9120, 9130, 9140, 9150, 9160, 9170);
