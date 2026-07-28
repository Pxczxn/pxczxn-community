SELECT CASE WHEN COUNT(DISTINCT index_name) = 5 THEN 'PASS' ELSE 'FAIL' END AS m5_search_indexes
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name IN (
    'idx_article_public_search',
    'idx_moment_public_search',
    'idx_blog_public_search',
    'idx_team_series_public_search',
    'idx_community_user_public_search'
  );
