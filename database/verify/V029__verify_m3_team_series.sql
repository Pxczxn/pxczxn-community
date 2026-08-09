SELECT COUNT(*) AS team_series_table_count
FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'team_series';

SELECT COUNT(*) AS team_series_article_table_count
FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'team_series_article';

SELECT COUNT(*) AS team_series_constraint_count
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'team_series_article'
  AND index_name IN ('uk_team_series_article_article', 'uk_team_series_article_order', 'idx_team_series_article_series');

SELECT `permission` AS series_review_permission
FROM `sys_menu` WHERE `id` = 9142 AND `deleted` = 0;
