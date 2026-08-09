SELECT COUNT(*) AS series_table_exists
FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'series';

SELECT COUNT(*) AS series_article_table_exists
FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'series_article';

SELECT COUNT(*) AS blog_id_column_exists
FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'series' AND column_name = 'blog_id';

SELECT COUNT(*) AS legacy_team_id_removed
FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'series' AND column_name = 'team_id';

SELECT COUNT(*) AS blog_slug_unique_exists
FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'series' AND index_name = 'uk_series_blog_slug';

SELECT COUNT(*) AS article_unique_exists
FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'series_article' AND index_name = 'uk_series_article_article';
