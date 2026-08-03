SELECT CASE WHEN
  (SELECT COUNT(*) FROM `sys_menu` WHERE `id`=9205 AND `parent_id`=0 AND `name`='账号治理' AND `type`=1 AND `icon`='ShieldOutline' AND `deleted`=0)=1
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id`=9194 AND `parent_id`=9205 AND `name`='申诉中心' AND `icon`='ChatbubblesOutline' AND `deleted`=0)=1
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id`=9191 AND `parent_id`=9205 AND `name`='账号处置' AND `icon`='HammerOutline' AND `deleted`=0)=1
  AND (SELECT COUNT(*) FROM `sys_menu` WHERE `id`=9199 AND `parent_id`=9205 AND `name`='账号操作中心' AND `icon`='KeyOutline' AND `deleted`=0)=1
THEN 'PASS' ELSE 'FAIL' END AS result;
