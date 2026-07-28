SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'creator_analytics_event') = 1
  AND (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'creator_analytics_event' AND index_name IN ('idx_creator_analytics_author_day','idx_creator_analytics_article_day')) = 2
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'creator_analytics_event' AND column_name IN ('ip','ip_address','user_agent','visitor_id','user_id')) = 0
THEN 'PASS' ELSE 'FAIL' END AS m5_creator_analytics;
