SELECT CASE WHEN EXISTS (
  SELECT 1
  FROM `sys_menu` m
  JOIN `sys_role_menu` rm ON rm.menu_id = m.id
  JOIN `sys_role` r ON r.id = rm.role_id
  WHERE m.id = 9190
    AND m.permission = 'community:series:review'
    AND r.code = 'admin'
    AND r.deleted = 0
) THEN 'PASS' ELSE 'FAIL' END AS team_series_review_permission;
