-- 账号处置菜单 9191 与账号操作中心 9199 的命名演变：
-- V055 命名为“社区处置 / 账号操作中心”，后续 V067/V069/V070 更名为“账号处置 / 账号管理”，
-- 并将 9191 并入“账号管理”目录（逻辑删除）。验证最终命名状态。
SELECT CASE WHEN COUNT(*) = 2 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE (`id` = 9199 AND `name` = '账号管理' AND `deleted` = 0)
   OR (`id` = 9191 AND `deleted` = 1);
