SELECT COUNT(*) AS appeal_tables
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('community_appeal', 'community_appeal_event');

SELECT COUNT(*) AS appeal_append_only_triggers
FROM information_schema.triggers
WHERE trigger_schema = DATABASE() AND event_object_table = 'community_appeal_event'
  AND trigger_name IN ('community_appeal_event_prevent_update', 'community_appeal_event_prevent_delete');
