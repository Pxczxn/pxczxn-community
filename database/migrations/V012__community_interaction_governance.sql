/*
 * Version: V012
 * Purpose: M2 管理端评论/动态治理事件、查询索引与 Mars Admin RBAC
 * Depends on: V011 and the Mars Admin sys_menu/sys_role/sys_role_menu tables
 * Locking: CREATE TABLE/CREATE INDEX/ALTER TABLE acquire metadata locks
 * Backup required: deployed environments must be backed up before execution
 * Repeatable: yes; all objects and role mappings are guarded
 */

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS `community_moment_moderation_event` (
    `id` BIGINT UNSIGNED NOT NULL,
    `moment_id` BIGINT UNSIGNED NOT NULL,
    `action` VARCHAR(32) NOT NULL,
    `actor_admin_id` BIGINT NOT NULL,
    `previous_status` VARCHAR(24) NOT NULL,
    `new_status` VARCHAR(24) NOT NULL,
    `reason` VARCHAR(500) NULL,
    `metadata_json` JSON NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_moment_moderation_moment_created`
        (`moment_id`, `created_at`, `id`),
    KEY `idx_moment_moderation_action_created`
        (`action`, `created_at`, `id`),
    CONSTRAINT `fk_moment_moderation_moment`
        FOREIGN KEY (`moment_id`)
        REFERENCES `community_moment` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_moment_moderation_action`
        CHECK (`action` IN (
            'PLATFORM_APPROVED', 'PLATFORM_REJECTED',
            'PLATFORM_TAKEN_DOWN', 'PLATFORM_RESTORED'
        ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='动态平台治理不可变事件';

/*
 * V009 的动作约束早于人工审核 API。本迁移仅移除旧约束一次，并使用新名称
 * 注册兼容审核通过/驳回的新动作；事件表仍然没有更新或删除接口。
 */
SET @old_comment_action_check_count := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_comment_moderation_event'
      AND CONSTRAINT_NAME = 'chk_comment_moderation_action'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @drop_old_comment_action_check_sql := IF(
    @old_comment_action_check_count > 0,
    'ALTER TABLE `community_comment_moderation_event`
       DROP CHECK `chk_comment_moderation_action`',
    'SELECT ''old comment moderation action check already removed'''
);
PREPARE drop_old_comment_action_check_stmt
    FROM @drop_old_comment_action_check_sql;
EXECUTE drop_old_comment_action_check_stmt;
DEALLOCATE PREPARE drop_old_comment_action_check_stmt;

SET @comment_action_check_v2_count := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_comment_moderation_event'
      AND CONSTRAINT_NAME = 'chk_comment_moderation_action_v2'
      AND CONSTRAINT_TYPE = 'CHECK'
);
SET @add_comment_action_check_v2_sql := IF(
    @comment_action_check_v2_count = 0,
    'ALTER TABLE `community_comment_moderation_event`
       ADD CONSTRAINT `chk_comment_moderation_action_v2`
       CHECK (`action` IN (
         ''AUTO_PUBLISHED'', ''AUTO_REVIEW_QUEUED'', ''USER_DELETED'',
         ''AUTHOR_HIDDEN'', ''BLOG_HIDDEN'', ''PLATFORM_APPROVED'',
         ''PLATFORM_REJECTED'', ''PLATFORM_TAKEN_DOWN'',
         ''PLATFORM_RESTORED''
       ))',
    'SELECT ''comment moderation action check v2 already exists'''
);
PREPARE add_comment_action_check_v2_stmt
    FROM @add_comment_action_check_v2_sql;
EXECUTE add_comment_action_check_v2_stmt;
DEALLOCATE PREPARE add_comment_action_check_v2_stmt;

SET @comment_governance_index_count := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community_comment'
      AND INDEX_NAME = 'idx_comment_admin_status_created'
);
SET @comment_governance_index_sql := IF(
    @comment_governance_index_count = 0,
    'CREATE INDEX `idx_comment_admin_status_created`
       ON `community_comment` (`status`, `created_at`, `id`)',
    'SELECT ''comment governance index already exists'''
);
PREPARE comment_governance_index_stmt
    FROM @comment_governance_index_sql;
EXECUTE comment_governance_index_stmt;
DEALLOCATE PREPARE comment_governance_index_stmt;

INSERT INTO `sys_menu` (
    `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`,
    `icon`, `sort`, `visible`, `status`, `is_frame`,
    `create_by`, `update_by`, `deleted`
) VALUES
    (9070, 9000, '评论治理', 2, '/community/comments',
     '/community/comments/index', 'community:comment:list',
     'ChatbubblesOutline', 5, 1, 1, 0, 1, 1, 0),
    (9071, 9070, '查询评论', 3, NULL, NULL,
     'community:comment:list', NULL, 1, 1, 1, 0, 1, 1, 0),
    (9072, 9070, '查看评论治理事件', 3, NULL, NULL,
     'community:comment:query', NULL, 2, 1, 1, 0, 1, 1, 0),
    (9073, 9070, '通过评论审核', 3, NULL, NULL,
     'community:comment:approve', NULL, 3, 1, 1, 0, 1, 1, 0),
    (9074, 9070, '驳回评论审核', 3, NULL, NULL,
     'community:comment:reject', NULL, 4, 1, 1, 0, 1, 1, 0),
    (9075, 9070, '下架评论', 3, NULL, NULL,
     'community:comment:takeDown', NULL, 5, 1, 1, 0, 1, 1, 0),
    (9076, 9070, '恢复评论', 3, NULL, NULL,
     'community:comment:restore', NULL, 6, 1, 1, 0, 1, 1, 0),
    (9077, 9070, '批量治理评论', 3, NULL, NULL,
     'community:comment:batch', NULL, 7, 1, 1, 0, 1, 1, 0),
    (9080, 9000, '动态治理', 2, '/community/moments',
     '/community/moments/index', 'community:moment:list',
     'PlanetOutline', 6, 1, 1, 0, 1, 1, 0),
    (9081, 9080, '查询动态', 3, NULL, NULL,
     'community:moment:list', NULL, 1, 1, 1, 0, 1, 1, 0),
    (9082, 9080, '查看动态治理事件', 3, NULL, NULL,
     'community:moment:query', NULL, 2, 1, 1, 0, 1, 1, 0),
    (9083, 9080, '通过动态审核', 3, NULL, NULL,
     'community:moment:approve', NULL, 3, 1, 1, 0, 1, 1, 0),
    (9084, 9080, '驳回动态审核', 3, NULL, NULL,
     'community:moment:reject', NULL, 4, 1, 1, 0, 1, 1, 0),
    (9085, 9080, '下架动态', 3, NULL, NULL,
     'community:moment:takeDown', NULL, 5, 1, 1, 0, 1, 1, 0),
    (9086, 9080, '恢复动态', 3, NULL, NULL,
     'community:moment:restore', NULL, 6, 1, 1, 0, 1, 1, 0),
    (9087, 9080, '批量治理动态', 3, NULL, NULL,
     'community:moment:batch', NULL, 7, 1, 1, 0, 1, 1, 0),
    (9090, 9000, '互动查询', 2, '/community/interactions',
     '/community/interactions/index', 'community:interaction:list',
     'PulseOutline', 7, 1, 1, 0, 1, 1, 0),
    (9091, 9090, '查询互动关系', 3, NULL, NULL,
     'community:interaction:list', NULL, 1, 1, 1, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `type` = VALUES(`type`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `permission` = VALUES(`permission`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `is_frame` = VALUES(`is_frame`),
    `update_by` = VALUES(`update_by`),
    `deleted` = VALUES(`deleted`);

UPDATE `sys_menu`
SET `sort` = 8, `update_by` = 1
WHERE `id` = 9020 AND `deleted` = 0;

UPDATE `sys_menu`
SET `sort` = 9, `update_by` = 1
WHERE `id` = 9010 AND `deleted` = 0;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT `r`.`id`, `menu_ids`.`menu_id`
FROM `sys_role` AS `r`
CROSS JOIN (
    SELECT 9000 AS `menu_id`
    UNION ALL SELECT 9070
    UNION ALL SELECT 9071
    UNION ALL SELECT 9072
    UNION ALL SELECT 9073
    UNION ALL SELECT 9074
    UNION ALL SELECT 9075
    UNION ALL SELECT 9076
    UNION ALL SELECT 9077
    UNION ALL SELECT 9080
    UNION ALL SELECT 9081
    UNION ALL SELECT 9082
    UNION ALL SELECT 9083
    UNION ALL SELECT 9084
    UNION ALL SELECT 9085
    UNION ALL SELECT 9086
    UNION ALL SELECT 9087
    UNION ALL SELECT 9090
    UNION ALL SELECT 9091
) AS `menu_ids`
WHERE `r`.`code` = 'admin'
  AND `r`.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `r`.`id`
        AND `existing`.`menu_id` = `menu_ids`.`menu_id`
  );
