SET NAMES utf8mb4;

SELECT CASE WHEN COUNT(*)=2 THEN 'PASS' ELSE 'FAIL' END AS chat_participant_deletion_columns
FROM information_schema.columns
WHERE table_schema=DATABASE()
  AND table_name='community_chat_message'
  AND column_name IN ('sender_deleted_at','recipient_deleted_at');

SELECT CASE WHEN COUNT(DISTINCT index_name)=2 THEN 'PASS' ELSE 'FAIL' END AS chat_participant_deletion_indexes
FROM information_schema.statistics
WHERE table_schema=DATABASE()
  AND table_name='community_chat_message'
  AND index_name IN ('idx_community_chat_sender_visibility','idx_community_chat_recipient_visibility');
