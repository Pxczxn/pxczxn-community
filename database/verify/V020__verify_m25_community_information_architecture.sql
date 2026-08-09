SET NAMES utf8mb4;

SELECT IF(
    COUNT(*) = 9,
    'PASS',
    'FAIL'
) AS `community_primary_menu_count`
FROM `sys_menu`
WHERE `id` IN (9000, 9100, 9110, 9120, 9130, 9140, 9150, 9160, 9170)
  AND `deleted` = 0
  AND `visible` = 1;

SELECT IF(
    COUNT(*) = 13,
    'PASS',
    'FAIL'
) AS `community_navigation_parenting`
FROM `sys_menu`
WHERE `deleted` = 0
  AND (
      (`id` IN (9040, 9050) AND `parent_id` = 9000)
      OR (`id` = 9060 AND `parent_id` = 9100)
      OR (`id` = 9020 AND `parent_id` = 9110)
      OR (`id` IN (9070, 9080, 9090) AND `parent_id` = 9120)
      OR (`id` = 9131 AND `parent_id` = 9130)
      OR (`id` IN (9010, 9141) AND `parent_id` = 9140)
      OR (`id` = 135 AND `parent_id` = 9150)
      OR (`id` = 139 AND `parent_id` = 9160)
      OR (`id` = 9171 AND `parent_id` = 9170)
  );

SELECT IF(
    COUNT(*) = 4,
    'PASS',
    'FAIL'
) AS `platform_personnel_navigation_parenting`
FROM `sys_menu`
WHERE `id` IN (2, 6, 23, 27)
  AND `parent_id` = 9180
  AND `deleted` = 0;

SELECT IF(
    COUNT(*) = 8,
    'PASS',
    'FAIL'
) AS `platform_group_copy_cleaned`
FROM `sys_dept`
WHERE `id` BETWEEN 1 AND 8
  AND `deleted` = 0
  AND `dept_name` IN (
      '星语社区平台', '技术运维组', '产品运营组', '内容审核组',
      '社区治理组', '平台运营组', '社区服务组', '技术支持组'
  );

SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `scaffold_operator_accounts_disabled`
FROM `sys_user`
WHERE `deleted` = 0
  AND LOWER(`username`) IN ('mars11', 'mars', 'lisi');
