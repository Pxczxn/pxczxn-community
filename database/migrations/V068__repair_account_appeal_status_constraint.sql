SET NAMES utf8mb4;

-- 修复账号申诉状态约束：允许两级申诉审核状态 PRIMARY_REVIEWED / FINALIZED。
-- 说明：原始 V068 文件为会话遗留、未纳入版本库，此处按幂等语义重建登记，
-- 保证迁移历史与仓库一致；本文件内容可安全重复执行。
SET @constraint_exists = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'community_account_enforcement_appeal'
    AND constraint_name = 'chk_account_enforcement_appeal_status'
    AND constraint_type = 'CHECK'
);
SET @sql = IF(@constraint_exists = 0,
  'ALTER TABLE `community_account_enforcement_appeal` ADD CONSTRAINT `chk_account_enforcement_appeal_status` CHECK ((`status` in (_utf8mb4''SUBMITTED'',_utf8mb4''UNDER_REVIEW'',_utf8mb4''UPHELD'',_utf8mb4''MODIFIED'',_utf8mb4''REVOKED'',_utf8mb4''FINAL'',_utf8mb4''PRIMARY_REVIEWED'',_utf8mb4''FINALIZED'')))',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
