SET NAMES utf8mb4;

SELECT
    CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS `table_check`,
    COUNT(*) AS `actual_table_count`
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'content_keyword_rule';

SELECT
    CASE WHEN COUNT(*) = 12 THEN 'PASS' ELSE 'FAIL' END AS `column_check`,
    COUNT(*) AS `actual_column_count`
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'content_keyword_rule'
  AND column_name IN (
      'id',
      'keyword',
      'normalized_keyword',
      'severity',
      'status',
      'description',
      'sort_order',
      'created_by_admin_id',
      'created_at',
      'updated_at',
      'deleted_at',
      'active_normalized_keyword'
  );

SELECT
    CASE WHEN COUNT(DISTINCT index_name) = 2 THEN 'PASS' ELSE 'FAIL' END
        AS `index_check`,
    COUNT(DISTINCT index_name) AS `actual_index_count`
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'content_keyword_rule'
  AND index_name IN (
      'uk_keyword_rule_active_normalized',
      'idx_keyword_rule_scan'
  );
