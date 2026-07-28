SELECT CASE WHEN COUNT(DISTINCT m.permission) = 2 THEN 'PASS' ELSE 'FAIL' END AS sanction_permissions
FROM `sys_menu` m
JOIN `sys_role_menu` rm ON rm.menu_id = m.id
JOIN `sys_role` r ON r.id = rm.role_id
WHERE m.id IN (9192, 9193)
  AND m.permission IN ('community:sanction:list', 'community:sanction:handle')
  AND r.code = 'admin'
  AND r.deleted = 0;
