SET NAMES utf8mb4;

SELECT COUNT(*) AS `notification_inbox_columns`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_notification'
  AND COLUMN_NAME IN (
      'category', 'importance', 'aggregate_count', 'last_activity_at'
  );

SELECT COUNT(DISTINCT INDEX_NAME) AS `notification_inbox_indexes`
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_notification'
  AND INDEX_NAME IN (
      'uk_notification_deduplication',
      'idx_notification_category_activity'
  );

SELECT COUNT(*) AS `notification_inbox_checks`
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_notification'
  AND CONSTRAINT_TYPE = 'CHECK'
  AND CONSTRAINT_NAME IN (
      'chk_notification_category',
      'chk_notification_importance',
      'chk_notification_aggregate_count'
  );

SELECT COUNT(*) AS `invalid_notification_rows`
FROM community_notification
WHERE category NOT IN (
          'INTERACTION', 'FOLLOW', 'COMMENT', 'COAUTHOR',
          'SUBMISSION', 'TEAM', 'REVIEW', 'SYSTEM'
      )
   OR importance NOT IN ('NORMAL', 'HIGH')
   OR aggregate_count < 1
   OR last_activity_at IS NULL;

SELECT COUNT(*) AS `duplicate_notification_keys`
FROM (
    SELECT deduplication_key
    FROM community_notification
    WHERE deduplication_key IS NOT NULL
    GROUP BY deduplication_key
    HAVING COUNT(*) > 1
) duplicates;
