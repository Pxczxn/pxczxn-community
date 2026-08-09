SELECT CASE WHEN COUNT(*) = 8 THEN 1 ELSE 0 END AS account_appeal_two_stage_columns_ready
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'community_account_enforcement_appeal'
  AND column_name IN ('primary_reviewed_by_admin_id','primary_decision','primary_review_note','primary_reviewed_at','final_reviewed_by_admin_id','final_decision','final_review_note','final_reviewed_at');

SELECT CASE WHEN EXISTS (
  SELECT 1 FROM sys_menu WHERE permission='community:account:list' AND name='账号管理' AND deleted=0
) THEN 1 ELSE 0 END AS account_management_menu_ready;

