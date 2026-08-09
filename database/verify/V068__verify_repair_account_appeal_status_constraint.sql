SET NAMES utf8mb4;

-- 账号申诉状态约束允许两级审核状态（PRIMARY_REVIEWED / FINALIZED）
SELECT CASE WHEN EXISTS (
  SELECT 1 FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'community_account_enforcement_appeal'
    AND constraint_name = 'chk_account_enforcement_appeal_status'
    AND constraint_type = 'CHECK'
) THEN 1 ELSE 0 END AS appeal_status_constraint_ready;

-- 状态枚举包含两级审核终态
SELECT CASE WHEN COUNT(*) = 8 THEN 1 ELSE 0 END AS appeal_status_enum_ready
FROM information_schema.check_constraints
WHERE constraint_schema = DATABASE()
  AND constraint_name = 'chk_account_enforcement_appeal_status'
  AND check_clause LIKE '%PRIMARY_REVIEWED%'
  AND check_clause LIKE '%FINALIZED%';
