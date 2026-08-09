SELECT COUNT(*) AS report_tables
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('community_report', 'community_report_event');

SELECT COUNT(*) AS report_indexes
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'community_report'
  AND index_name IN ('uk_report_active_dedupe', 'idx_report_queue', 'idx_report_target', 'idx_report_reporter');

SELECT COUNT(*) AS report_append_only_triggers
FROM information_schema.triggers
WHERE trigger_schema = DATABASE() AND event_object_table = 'community_report_event'
  AND trigger_name IN ('community_report_event_prevent_update', 'community_report_event_prevent_delete');
