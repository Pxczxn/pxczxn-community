SELECT COUNT(*) AS abuse_tables FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('community_abuse_window','community_abuse_event');
SELECT COUNT(*) AS abuse_triggers FROM information_schema.triggers WHERE trigger_schema=DATABASE() AND event_object_table='community_abuse_event';
