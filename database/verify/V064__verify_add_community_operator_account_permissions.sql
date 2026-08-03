SET NAMES utf8mb4;

SELECT CASE WHEN EXISTS (SELECT 1 FROM `sys_role` WHERE `code`='community_admin' AND `status`=1 AND `deleted`=0)
  THEN 'PASS' ELSE 'FAIL' END AS community_admin_role;

SELECT CASE WHEN COUNT(*)=5 THEN 'PASS' ELSE 'FAIL' END AS community_admin_account_menu_grants
FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.id=rm.role_id
WHERE r.code='community_admin' AND rm.menu_id IN (9205,9199,9200,9201,9202);
