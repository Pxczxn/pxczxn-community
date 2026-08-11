SET NAMES utf8mb4;

SELECT CASE
    WHEN COUNT(*) = 3 THEN 'PASS'
    ELSE 'FAIL'
END AS series_table_comments_localized
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_NAME IN ('series', 'series_article', 'series_reading_progress')
  AND TABLE_COMMENT <> ''
  AND TABLE_COMMENT NOT REGEXP '^[[:ascii:]]+$';
