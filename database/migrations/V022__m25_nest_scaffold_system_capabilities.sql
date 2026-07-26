/*
 * Version: V022
 * Purpose: Keep retained scaffold capabilities out of the community primary menu.
 * Depends on: V021 M2.5 sidebar ordering.
 * Repeatable: Yes.
 *
 * Monitoring, audit, file and developer tools remain available under System
 * Settings. Their paths, permissions and component bindings are unchanged.
 */

SET NAMES utf8mb4;

UPDATE `sys_menu`
SET `parent_id` = 1,
    `name` = CASE `id`
        WHEN 31 THEN '系统运行与审计'
        WHEN 36 THEN '平台监控'
        WHEN 126 THEN '文件与存储'
        WHEN 161 THEN '开发工具'
        ELSE `name`
    END,
    `sort` = CASE `id`
        WHEN 31 THEN 20
        WHEN 36 THEN 21
        WHEN 126 THEN 22
        WHEN 161 THEN 23
        ELSE `sort`
    END,
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = 1
WHERE `id` IN (31, 36, 126, 161)
  AND `deleted` = 0;
