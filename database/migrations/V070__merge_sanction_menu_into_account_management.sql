SET NAMES utf8mb4;

-- 将“账号处置”菜单（9191 及其子菜单 9192/9193）合并进“账号管理”（9199）：
-- 账号处置页面已并入账号管理页的“账号处置”tab，不再作为独立菜单展示。
-- 使用逻辑删除（deleted=1）而非物理删除，保留 sys_role_menu 关联以便回滚。
UPDATE `sys_menu`
SET `deleted` = 1,
    `update_time` = CURRENT_TIMESTAMP
WHERE (`id` = 9191 OR `parent_id` = 9191)
  AND `deleted` = 0;

-- 确认“账号管理”菜单命名与排序（置于账号治理分组首位）
UPDATE `sys_menu`
SET `name` = '账号管理',
    `sort` = 1,
    `update_time` = CURRENT_TIMESTAMP
WHERE `id` = 9199
  AND `deleted` = 0;
