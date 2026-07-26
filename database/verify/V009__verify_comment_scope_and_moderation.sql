SET NAMES utf8mb4;

SELECT COUNT(*) AS comment_moderation_tables
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_comment_moderation_event';

SELECT
    COUNT(*) AS moderation_required_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_comment_moderation_event'
  AND COLUMN_NAME IN (
      'comment_id', 'action', 'actor_type', 'previous_status',
      'new_status', 'reason', 'metadata_json', 'created_at'
  );

SELECT COUNT(*) AS comment_scope_check_constraints
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'blog_setting'
  AND CONSTRAINT_NAME = 'chk_blog_setting_comment_scope'
  AND CONSTRAINT_TYPE = 'CHECK';

SELECT COUNT(*) AS invalid_comment_scope_rows
FROM blog_setting
WHERE comment_scope NOT IN (
    'ALL_LOGGED_IN', 'FOLLOWERS_ONLY', 'MUTUAL_ONLY',
    'BLOGGER_FOLLOWING', 'TEAM_FOLLOWERS', 'TEAM_MEMBERS', 'DISABLED'
);
