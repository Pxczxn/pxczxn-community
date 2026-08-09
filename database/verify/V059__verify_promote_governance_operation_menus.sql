-- 治理与操作菜单提升为一级菜单：V059 将申诉中心/账号处置/账号操作中心提升到根级，
-- 后续 V060 统一归入“账号治理”目录（9205）。验证最终归属。
SELECT CASE WHEN COUNT(*) = 2 THEN 'PASS' ELSE 'FAIL' END AS result
FROM `sys_menu`
WHERE `deleted` = 0
  AND (
       (`id` = 9194 AND `parent_id` = 9205 AND `name` = '申诉中心')
    OR (`id` = 9199 AND `parent_id` = 9205 AND `name` = '账号管理')
  );
