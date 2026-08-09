SET NAMES utf8mb4;

SELECT CASE
  WHEN COUNT(*) = 0 THEN 'PASS'
  ELSE 'FAIL'
END AS all_base_tables_have_localized_comments
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND (
    TABLE_COMMENT = ''
    OR TABLE_COMMENT REGEXP '^[[:ascii:]]+$'
    OR TABLE_COMMENT LIKE '%?%'
  );

SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_NAME IN (
    'article_collaboration_audit_event',
    'article_collaboration_invitation',
    'article_collaborator',
    'community_appeal',
    'community_appeal_event',
    'community_block',
    'community_chat_message',
    'community_moment_moderation_event',
    'community_report',
    'community_report_event',
    'content_keyword_rule',
    'team',
    'team_application',
    'team_audit_event',
    'team_invitation',
    'team_member',
    'team_permission',
    'team_role',
    'team_series',
    'team_series_article',
    'team_submission'
  )
ORDER BY TABLE_NAME;
