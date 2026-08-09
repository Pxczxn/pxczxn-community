SELECT COUNT(*) AS article_collaboration_tables
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('article_collaboration_invitation', 'article_collaborator', 'article_collaboration_audit_event');

SELECT COUNT(*) AS article_collaboration_required_indexes
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('article_collaboration_invitation', 'article_collaborator')
  AND index_name IN ('uk_article_collab_invitation_idempotency', 'uk_article_collab_active_invitee', 'uk_article_collaborator_active_user', 'uk_article_collaborator_active_order');
