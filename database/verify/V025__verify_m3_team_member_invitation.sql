SET NAMES utf8mb4;

SELECT COUNT(*) AS idempotency_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_invitation'
  AND COLUMN_NAME = 'idempotency_key';

SELECT COUNT(*) AS idempotency_index_count
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_invitation'
  AND INDEX_NAME = 'uk_team_invitation_idempotency';

SELECT COUNT(*) AS pending_invitation_unique_index_count
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_invitation'
  AND INDEX_NAME = 'uk_pending_invitation'
  AND COLUMN_NAME = 'is_pending';
