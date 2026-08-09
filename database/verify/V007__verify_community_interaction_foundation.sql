SET NAMES utf8mb4;

SELECT
    CASE WHEN COUNT(*) = 7 THEN 'PASS' ELSE 'FAIL' END
        AS `interaction_table_count_check`,
    COUNT(*) AS `actual_table_count`
FROM `information_schema`.`tables`
WHERE `table_schema` = DATABASE()
  AND `table_name` IN (
      'community_follow',
      'community_moment',
      'community_comment',
      'community_content_like',
      'favorite_folder',
      'favorite_item',
      'favorite_folder_item'
  );

SELECT
    CASE WHEN COUNT(*) = 8 THEN 'PASS' ELSE 'FAIL' END
        AS `follow_column_count_check`,
    COUNT(*) AS `actual_column_count`
FROM `information_schema`.`columns`
WHERE `table_schema` = DATABASE()
  AND `table_name` = 'community_follow'
  AND `column_name` IN (
      'id', 'follower_user_id', 'target_type', 'target_id',
      'notification_level', 'special_follow', 'created_at', 'updated_at'
  );

SELECT
    CASE
        WHEN COUNT(DISTINCT CONCAT(`table_name`, '.', `index_name`)) = 5
        THEN 'PASS'
        ELSE 'FAIL'
    END
        AS `interaction_unique_constraint_check`,
    COUNT(DISTINCT CONCAT(`table_name`, '.', `index_name`))
        AS `actual_unique_constraints`
FROM `information_schema`.`statistics`
WHERE `table_schema` = DATABASE()
  AND `index_name` IN (
      'uk_community_follow_target',
      'uk_content_like_user_target',
      'uk_favorite_folder_default',
      'uk_favorite_item_owner_target',
      'uk_favorite_folder_item'
  )
  AND `non_unique` = 0;

SELECT
    CASE WHEN COUNT(*) = 14 THEN 'PASS' ELSE 'FAIL' END
        AS `interaction_foreign_key_check`,
    COUNT(*) AS `actual_foreign_keys`
FROM `information_schema`.`table_constraints`
WHERE `constraint_schema` = DATABASE()
  AND `table_name` IN (
      'community_follow',
      'community_moment',
      'community_comment',
      'community_content_like',
      'favorite_folder',
      'favorite_item',
      'favorite_folder_item'
  )
  AND `constraint_type` = 'FOREIGN KEY';
