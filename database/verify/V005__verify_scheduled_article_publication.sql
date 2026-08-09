SET NAMES utf8mb4;

SELECT COUNT(*) AS `publish_task_table_count`
FROM `information_schema`.`tables`
WHERE `table_schema` = DATABASE()
  AND `table_name` = 'article_publish_task';

SELECT
    COUNT(*) AS `required_column_count`
FROM `information_schema`.`columns`
WHERE `table_schema` = DATABASE()
  AND `table_name` = 'article_publish_task'
  AND `column_name` IN (
      'article_id',
      'article_version_id',
      'scheduled_publish_at',
      'status',
      'attempt_count',
      'max_attempts',
      'next_attempt_at',
      'last_error_code',
      'last_error_message',
      'lock_version',
      'active_article_id'
  );

SELECT
    `job_group`,
    `invoke_target`,
    `cron_expression`,
    `concurrent`,
    `status`
FROM `sys_job`
WHERE `invoke_target` = 'articleScheduledPublishTask.runDueBatch'
  AND `deleted` = 0;
