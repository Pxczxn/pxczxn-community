SET NAMES utf8mb4;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `name` = '申诉中心',
    `icon` = 'ChatbubblesOutline',
    `sort` = 5,
    `update_time` = CURRENT_TIMESTAMP
WHERE `id` = 9194;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `name` = '账号处置',
    `icon` = 'HammerOutline',
    `sort` = 6,
    `update_time` = CURRENT_TIMESTAMP
WHERE `id` = 9191;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `name` = '账号操作中心',
    `icon` = 'KeyOutline',
    `sort` = 7,
    `update_time` = CURRENT_TIMESTAMP
WHERE `id` = 9199;
