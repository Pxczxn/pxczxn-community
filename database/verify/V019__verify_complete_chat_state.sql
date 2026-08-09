SET NAMES utf8mb4;

SELECT IF(
    COUNT(*) = 4,
    'PASS',
    'FAIL'
) AS `chat_state_columns_available`
FROM `information_schema`.`COLUMNS`
WHERE `TABLE_SCHEMA` = DATABASE()
  AND (
      (`TABLE_NAME` = 'sys_chat_message'
          AND `COLUMN_NAME` IN ('sender_deleted', 'receiver_deleted'))
      OR
      (`TABLE_NAME` = 'sys_chat_group_member'
          AND `COLUMN_NAME` IN ('last_read_message_id', 'last_read_time'))
  );

SELECT IF(
    COUNT(DISTINCT `INDEX_NAME`) = 3,
    'PASS',
    'FAIL'
) AS `chat_query_indexes_available`
FROM `information_schema`.`STATISTICS`
WHERE `TABLE_SCHEMA` = DATABASE()
  AND (
      (`TABLE_NAME` = 'sys_chat_message'
          AND `INDEX_NAME` IN ('idx_chat_pair_time', 'idx_chat_receiver_unread'))
      OR
      (`TABLE_NAME` = 'sys_chat_group_message'
          AND `INDEX_NAME` = 'idx_chat_group_message_page')
  );

SELECT IF(
    COUNT(*) = 6,
    'PASS',
    'FAIL'
) AS `chat_state_checks_available`
FROM `information_schema`.`TABLE_CONSTRAINTS`
WHERE `TABLE_SCHEMA` = DATABASE()
  AND `CONSTRAINT_TYPE` = 'CHECK'
  AND `CONSTRAINT_NAME` IN (
      'chk_chat_message_type',
      'chk_chat_message_read',
      'chk_chat_message_visibility',
      'chk_chat_group_member_role',
      'chk_chat_group_member_muted',
      'chk_chat_group_message_type'
  );

SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `chat_read_cursor_initialized`
FROM `sys_chat_group_member` `member`
LEFT JOIN (
    SELECT `group_id`, MAX(`id`) AS `latest_message_id`
    FROM `sys_chat_group_message`
    GROUP BY `group_id`
) `latest` ON `latest`.`group_id` = `member`.`group_id`
WHERE `member`.`last_read_message_id` < COALESCE(`latest`.`latest_message_id`, 0);
