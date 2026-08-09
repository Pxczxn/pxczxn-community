-- 审核中心 / 账号处置 / 账号管理 三个菜单图标区分。
-- 9120 图标后续由 V057 调整为 ShieldOutline，此处验证最终生效的图标。
SELECT CASE WHEN COUNT(*) = 3 THEN 'PASS' ELSE 'FAIL' END AS result
FROM sys_menu
WHERE (`id` = 9020 AND `icon` = 'DocumentTextOutline')
   OR (`id` = 9120 AND `icon` = 'ShieldOutline')
   OR (`id` = 9199 AND `icon` = 'KeyOutline');
