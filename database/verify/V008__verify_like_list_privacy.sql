SET NAMES utf8mb4;

SELECT
    COUNT(*) AS likes_visibility_columns,
    MAX(IS_NULLABLE = 'NO') AS is_not_nullable,
    MAX(COLUMN_DEFAULT = 'PRIVATE') AS default_is_private
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_user_preference'
  AND COLUMN_NAME = 'likes_visibility';

SELECT COUNT(*) AS likes_visibility_check_constraints
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_user_preference'
  AND CONSTRAINT_NAME = 'chk_user_preference_likes_visibility'
  AND CONSTRAINT_TYPE = 'CHECK';

SELECT COUNT(*) AS invalid_likes_visibility_rows
FROM community_user_preference
WHERE likes_visibility NOT IN (
    'PRIVATE', 'PUBLIC', 'FOLLOWERS_ONLY', 'MUTUAL_ONLY'
);
