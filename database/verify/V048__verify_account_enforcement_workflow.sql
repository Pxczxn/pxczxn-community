SELECT CASE WHEN COUNT(*) = 4 THEN 'PASS' ELSE 'FAIL' END AS result
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
    'community_account_enforcement_case',
    'community_account_enforcement_review',
    'community_account_enforcement_appeal',
    'community_account_enforcement_event'
  );
