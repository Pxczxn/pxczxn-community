/*
 * Version: V020
 * Purpose: Reframe the operational information architecture as a community
 *          control center and replace scaffold sample copy with platform terms.
 * Depends on: V019 and the existing sys_menu/sys_role/sys_role_menu schema.
 * Repeatable: Yes. Fixed menu ids and guarded role mappings make it idempotent.
 *
 * This migration changes presentation and navigation only. It does not change
 * RBAC permissions, public API paths, community content records, or sys_* table
 * names. M3/M5 entries are explicit planned pages, not fake operational data.
 */

SET NAMES utf8mb4;

INSERT INTO `sys_menu` (
    `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`,
    `icon`, `sort`, `visible`, `status`, `is_frame`,
    `create_time`, `update_time`, `create_by`, `update_by`, `deleted`
) VALUES
    (9100, 0, '内容管理', 1, '/content', NULL, NULL,
     'DocumentTextOutline', 3, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9110, 0, '审核中心', 1, '/review-center', NULL, NULL,
     'ShieldCheckmarkOutline', 4, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9120, 0, '社区治理', 1, '/governance', NULL, NULL,
     'ShieldOutline', 5, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9130, 0, '团队管理', 1, '/teams', NULL, NULL,
     'PeopleOutline', 6, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9140, 0, '标签与系列', 1, '/taxonomy', NULL, NULL,
     'PricetagOutline', 7, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9150, 0, '通知管理', 1, '/notifications', NULL, NULL,
     'NotificationsOutline', 8, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9160, 0, '即时聊天', 1, '/chat', NULL, NULL,
     'ChatbubbleOutline', 9, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9170, 0, '数据分析', 1, '/analytics', NULL, NULL,
     'PulseOutline', 10, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9180, 1, '平台人员与权限', 1, '/system/platform-access', NULL, NULL,
     'PeopleOutline', 1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9131, 9130, '团队管理', 2, '/community/teams', '/community/planned/index',
     'community:team:list', 'PeopleOutline', 1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9141, 9140, '系列管理', 2, '/community/series', '/community/planned/index',
     'community:series:list', 'AlbumsOutline', 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9171, 9170, '社区数据分析', 2, '/community/analytics', '/community/planned/index',
     'community:analytics:view', 'PulseOutline', 1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `type` = VALUES(`type`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `permission` = VALUES(`permission`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `is_frame` = VALUES(`is_frame`),
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = VALUES(`update_by`),
    `deleted` = VALUES(`deleted`);

UPDATE `sys_menu`
SET `name` = '用户与博客', `sort` = 2, `icon` = 'PeopleOutline', `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` = 9000 AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 9000, `name` = CASE `id`
        WHEN 9040 THEN '社区用户'
        WHEN 9050 THEN '博客与创作者'
        ELSE `name`
    END,
    `sort` = CASE `id` WHEN 9040 THEN 1 WHEN 9050 THEN 2 ELSE `sort` END,
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = 1
WHERE `id` IN (9040, 9050) AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 9100, `name` = '文章管理', `sort` = 1,
    `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` = 9060 AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 9110, `name` = '文章审核', `sort` = 1,
    `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` = 9020 AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 9120, `sort` = CASE `id` WHEN 9070 THEN 1 WHEN 9080 THEN 2 WHEN 9090 THEN 3 ELSE `sort` END,
    `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` IN (9070, 9080, 9090) AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 9140, `name` = '平台标签', `sort` = 1,
    `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` = 9010 AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 9150, `name` = '平台通知', `sort` = 1,
    `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` = 135 AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 9160, `name` = '聊天会话', `sort` = 1,
    `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` = 139 AND `deleted` = 0;

UPDATE `sys_menu`
SET `name` = '系统设置', `sort` = 99, `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` = 1 AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 9180,
    `name` = CASE `id`
        WHEN 2 THEN '平台人员'
        WHEN 6 THEN '平台角色与权限'
        WHEN 23 THEN '平台组织'
        WHEN 27 THEN '平台岗位'
        ELSE `name`
    END,
    `sort` = CASE `id` WHEN 2 THEN 1 WHEN 6 THEN 2 WHEN 23 THEN 3 WHEN 27 THEN 4 ELSE `sort` END,
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = 1
WHERE `id` IN (2, 6, 23, 27) AND `deleted` = 0;

/* Empty legacy groups must not appear as parallel company-management menus. */
UPDATE `sys_menu`
SET `visible` = 0, `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` IN (22, 134) AND `deleted` = 0;

/* M2.5 routes are source-of-truth menu labels; M3/M5 pages self-identify as planned. */
UPDATE `sys_menu`
SET `name` = CASE `path`
        WHEN '/community/dashboard' THEN '运营总览'
        WHEN '/community/users' THEN '社区用户'
        WHEN '/community/blogs' THEN '博客与创作者'
        WHEN '/community/articles' THEN '文章管理'
        WHEN '/community/comments' THEN '评论治理'
        WHEN '/community/moments' THEN '动态治理'
        WHEN '/community/interactions' THEN '互动查询'
        WHEN '/community/reviews' THEN '文章审核'
        WHEN '/community/tags' THEN '平台标签'
        ELSE `name`
    END,
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = 1
WHERE `path` LIKE '/community/%' AND `deleted` = 0;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT `r`.`id`, `menu_ids`.`menu_id`
FROM `sys_role` AS `r`
CROSS JOIN (
    SELECT 9100 AS `menu_id` UNION ALL SELECT 9110 UNION ALL SELECT 9120
    UNION ALL SELECT 9130 UNION ALL SELECT 9131 UNION ALL SELECT 9140
    UNION ALL SELECT 9141 UNION ALL SELECT 9150 UNION ALL SELECT 9160
    UNION ALL SELECT 9170 UNION ALL SELECT 9171 UNION ALL SELECT 9180
) AS `menu_ids`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `r`.`id`
        AND `existing`.`menu_id` = `menu_ids`.`menu_id`
  );

/* Replace local scaffold sample names with the platform's real operating groups. */
UPDATE `sys_dept`
SET `dept_name` = CASE `id`
        WHEN 1 THEN '星语社区平台'
        WHEN 2 THEN '技术运维组'
        WHEN 3 THEN '产品运营组'
        WHEN 4 THEN '内容审核组'
        WHEN 5 THEN '社区治理组'
        WHEN 6 THEN '平台运营组'
        WHEN 7 THEN '社区服务组'
        WHEN 8 THEN '技术支持组'
        ELSE `dept_name`
    END,
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = 1
WHERE `id` BETWEEN 1 AND 8 AND `deleted` = 0;

UPDATE `sys_post`
SET `post_name` = CASE `id`
        WHEN 1 THEN '平台负责人'
        WHEN 2 THEN '技术运维负责人'
        WHEN 3 THEN '内容运营专员'
        WHEN 4 THEN '平台研发工程师'
        WHEN 6 THEN '社区运营负责人'
        WHEN 8 THEN '内容审核专员'
        ELSE `post_name`
    END,
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = 1
WHERE `id` IN (1, 2, 3, 4, 6, 8) AND `deleted` = 0;

/* The local admin remains available; scaffold accounts are not community operators. */
UPDATE `sys_user`
SET `status` = 0, `deleted` = 1, `update_time` = CURRENT_TIMESTAMP, `update_by` = 1
WHERE `id` <> 1
  AND LOWER(`username`) IN ('mars11', 'mars', 'lisi')
  AND `deleted` = 0;
