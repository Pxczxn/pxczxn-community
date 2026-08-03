SELECT CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS result
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_account_enforcement_case'
  AND CONSTRAINT_NAME = 'chk_account_enforcement_measure';
