SET NAMES utf8mb4;

SELECT CASE WHEN COUNT(*) = 1 THEN 1 ELSE 0 END AS creator_idea_table_ready
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'community_creator_idea';

SELECT CASE WHEN COUNT(*) = 6 THEN 1 ELSE 0 END AS creator_idea_columns_ready
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'community_creator_idea'
  AND column_name IN ('owner_user_id', 'title', 'content', 'tags_text', 'source_type', 'deleted_at');

SELECT CASE WHEN EXISTS (
  SELECT 1 FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'community_creator_idea'
    AND index_name = 'idx_creator_idea_owner_created'
) THEN 1 ELSE 0 END AS creator_idea_index_ready;
