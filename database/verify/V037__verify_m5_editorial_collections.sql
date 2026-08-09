SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('editorial_collection','editorial_collection_item')) = 2
  AND (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'editorial_collection' AND index_name = 'idx_editorial_public') = 1
  AND (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'editorial_collection_item' AND index_name = 'idx_editorial_item_order') = 1
  AND (SELECT COUNT(*) FROM sys_menu WHERE id IN (9140,9141) AND deleted = 0) = 2
THEN 'PASS' ELSE 'FAIL' END AS m5_editorial_collections;
