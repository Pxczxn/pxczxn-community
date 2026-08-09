SET NAMES utf8mb4;

-- 表已创建
SELECT CASE WHEN COUNT(*) = 1 THEN 1 ELSE 0 END AS series_reading_progress_table_ready
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'series_reading_progress';

-- 列就绪(用户/系列/最后文章/最后章节序/最远章节序/更新时间)
SELECT CASE WHEN COUNT(*) = 6 THEN 1 ELSE 0 END AS series_reading_progress_columns_ready
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'series_reading_progress'
  AND column_name IN (
    'user_id', 'series_id', 'last_article_id', 'last_chapter_order', 'max_chapter_order', 'updated_at'
  );

-- 用户与系列唯一
SELECT CASE WHEN EXISTS (
  SELECT 1 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'series_reading_progress'
    AND index_name = 'uk_series_reading_progress_user_series' AND non_unique = 0
) THEN 1 ELSE 0 END AS series_reading_progress_unique_ready;

-- 外键指向泛化后的 series 表
SELECT CASE WHEN EXISTS (
  SELECT 1 FROM information_schema.referential_constraints
  WHERE constraint_schema = DATABASE()
    AND constraint_name = 'fk_series_reading_progress_series'
    AND referenced_table_name = 'series'
) THEN 1 ELSE 0 END AS series_reading_progress_fk_ready;
