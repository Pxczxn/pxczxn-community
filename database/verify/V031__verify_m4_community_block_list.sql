SELECT COUNT(*) AS community_block_tables
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'community_block';

SELECT COUNT(*) AS community_block_indexes
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'community_block'
  AND index_name IN ('uk_community_block_target', 'idx_community_block_blocker', 'idx_community_block_target');
