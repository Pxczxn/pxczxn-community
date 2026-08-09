SELECT CASE WHEN EXISTS (
  SELECT 1
  FROM `sys_role` r
  JOIN `sys_role_menu` rm ON rm.role_id=r.id
  JOIN `sys_menu` m ON m.id=rm.menu_id
  WHERE r.code='community_admin'
    AND r.deleted=0
    AND m.permission='community:account:approve'
    AND m.deleted=0
) THEN 1 ELSE 0 END AS account_initial_review_permission_ready;
