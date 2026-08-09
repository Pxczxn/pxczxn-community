-- 账号治理目录 9205 聚合治理菜单：V060 将申诉中心/账号处置/账号操作中心归入目录。
-- 账号处置 9191 后续由 V070 并入“账号管理”目录（逻辑删除），不再要求其存在。
SELECT CASE WHEN
  (SELECT COUNT(*) FROM `sys_menu` WHERE `id`=9205 AND `parent_id`=0 AND `name`='账号治理' AND `type`=1 AND `icon`='GitBranchOutline' AND `deleted`=0)=1
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id`=9194 AND `parent_id`=9205 AND `name`='申诉中心' AND `icon`='ChatbubblesOutline' AND `deleted`=0)=1
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id`=9199 AND `parent_id`=9205 AND `name`='账号管理' AND `icon`='KeyOutline' AND `deleted`=0)=1
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id`=9191 AND `deleted`=0)=0
THEN 'PASS' ELSE 'FAIL' END AS result;
