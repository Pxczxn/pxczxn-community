SET NAMES utf8mb4;

-- 将账号治理下的菜单重命名为“账号管理”。
-- 说明：原始 V069 文件为会话遗留、未纳入版本库，此处按幂等语义重建登记。
UPDATE `sys_menu`
SET `name` = '账号管理',
    `update_time` = CURRENT_TIMESTAMP
WHERE `id` = 9199
  AND `path` = '/community/account-enforcements'
  AND `deleted` = 0;
