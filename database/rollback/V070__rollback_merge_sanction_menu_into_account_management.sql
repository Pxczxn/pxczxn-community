SET NAMES utf8mb4;

-- 回滚：恢复“账号处置”菜单及其子菜单（保留的 sys_role_menu 关联一并生效），并恢复账号管理菜单原排序
UPDATE `sys_menu`
SET `deleted` = 0,
    `update_time` = CURRENT_TIMESTAMP
WHERE (`id` = 9191 OR `parent_id` = 9191)
  AND `deleted` = 1;

UPDATE `sys_menu`
SET `sort` = 3,
    `update_time` = CURRENT_TIMESTAMP
WHERE `id` = 9199
  AND `deleted` = 0;
