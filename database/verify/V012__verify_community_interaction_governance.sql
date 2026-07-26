SET NAMES utf8mb4;

SELECT COUNT(*) AS `moment_moderation_event_table`
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_moment_moderation_event';

SELECT COUNT(*) AS `moment_moderation_event_columns`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_moment_moderation_event'
  AND COLUMN_NAME IN (
      'id', 'moment_id', 'action', 'actor_admin_id',
      'previous_status', 'new_status', 'reason',
      'metadata_json', 'created_at'
  );

SELECT COUNT(*) AS `governance_checks`
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_TYPE = 'CHECK'
  AND (
      (
          TABLE_NAME = 'community_comment_moderation_event'
          AND CONSTRAINT_NAME = 'chk_comment_moderation_action_v2'
      )
      OR (
          TABLE_NAME = 'community_moment_moderation_event'
          AND CONSTRAINT_NAME = 'chk_moment_moderation_action'
      )
  );

SELECT COUNT(*) AS `governance_menu_rows`
FROM `sys_menu`
WHERE `id` BETWEEN 9070 AND 9091
  AND `deleted` = 0;

SELECT COUNT(*) AS `admin_governance_permissions`
FROM `sys_role_menu` AS `rm`
JOIN `sys_role` AS `r` ON `r`.`id` = `rm`.`role_id`
JOIN `sys_menu` AS `m` ON `m`.`id` = `rm`.`menu_id`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND `m`.`id` BETWEEN 9070 AND 9091
  AND `m`.`deleted` = 0;

SELECT COUNT(*) AS `invalid_moment_moderation_events`
FROM `community_moment_moderation_event`
WHERE `action` NOT IN (
          'PLATFORM_APPROVED', 'PLATFORM_REJECTED',
          'PLATFORM_TAKEN_DOWN', 'PLATFORM_RESTORED'
      )
   OR `previous_status` = `new_status`;
