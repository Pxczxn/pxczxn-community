SELECT COUNT(*) AS team_submission_table_exists
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'team_submission';

SELECT COUNT(*) AS team_submission_required_columns
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'team_submission'
  AND column_name IN ('source_article_id', 'fixed_source_version_id', 'target_team_id',
                      'supersedes_submission_id', 'published_team_article_id',
                      'idempotency_key', 'lock_version');

SELECT COUNT(*) AS team_submission_required_indexes
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'team_submission'
  AND index_name IN ('uk_team_submission_idempotency',
                     'uk_team_submission_active_source_target',
                     'uk_team_submission_published_article');
