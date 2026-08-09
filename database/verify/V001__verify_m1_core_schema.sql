SET NAMES utf8mb4;

SELECT
    CASE WHEN COUNT(*) = 15 THEN 'PASS' ELSE 'FAIL' END AS `table_count_check`,
    COUNT(*) AS `actual_table_count`
FROM `information_schema`.`tables`
WHERE `table_schema` = DATABASE()
  AND `table_name` IN (
      'community_user',
      'community_user_login_account',
      'community_user_preference',
      'blog',
      'blog_setting',
      'blog_category',
      'article',
      'article_version',
      'platform_tag',
      'article_tag',
      'content_review_task',
      'community_notification',
      'community_notification_recipient',
      'file_object',
      'community_file_reference'
  );

SELECT
    `table_name`,
    COUNT(*) AS `index_count`
FROM `information_schema`.`statistics`
WHERE `table_schema` = DATABASE()
  AND `table_name` IN (
      'community_user',
      'community_user_login_account',
      'blog',
      'blog_category',
      'article',
      'article_version',
      'platform_tag',
      'article_tag',
      'content_review_task'
  )
GROUP BY `table_name`
ORDER BY `table_name`;

SELECT
    `table_name`,
    `constraint_name`,
    `constraint_type`
FROM `information_schema`.`table_constraints`
WHERE `constraint_schema` = DATABASE()
  AND `table_name` IN (
      'community_user_login_account',
      'blog',
      'blog_category',
      'article',
      'article_version',
      'article_tag',
      'content_review_task'
  )
ORDER BY `table_name`, `constraint_type`, `constraint_name`;
