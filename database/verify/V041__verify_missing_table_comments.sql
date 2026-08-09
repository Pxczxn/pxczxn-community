SET NAMES utf8mb4;

SELECT CASE
  WHEN COUNT(*) = 0 THEN 'PASS'
  ELSE 'FAIL'
END AS all_base_tables_have_comments
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_COMMENT = '';

SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_NAME IN (
    'coder_banner',
    'community_abuse_event',
    'community_abuse_window',
    'community_sanction',
    'community_sanction_event',
    'community_sanction_rate_limit',
    'creator_analytics_event',
    'editorial_collection',
    'editorial_collection_item',
    'pxczxn_schema_version'
  )
ORDER BY TABLE_NAME;
