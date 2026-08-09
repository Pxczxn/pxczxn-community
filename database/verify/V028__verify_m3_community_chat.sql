SELECT COUNT(*) AS community_chat_table_count FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'community_chat_message';
SELECT COUNT(*) AS community_chat_index_count FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'community_chat_message' AND index_name IN ('idx_community_chat_recipient', 'idx_community_chat_pair');
