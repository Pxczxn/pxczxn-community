/* M4-T001: report intake, operations queue, and immutable handling events. */
SET NAMES utf8mb4;

CREATE TABLE `community_report` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT 'Report ID',
    `reporter_user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Community reporter',
    `target_type` VARCHAR(24) NOT NULL COMMENT 'ARTICLE, MOMENT, COMMENT, BLOG, USER, TEAM, CHAT',
    `target_id` BIGINT UNSIGNED NOT NULL COMMENT 'Reported resource identifier',
    `reason_code` VARCHAR(40) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `evidence_json` JSON NULL COMMENT 'Reporter supplied evidence references only',
    `status` VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    `assignee_admin_id` BIGINT NULL,
    `resolution_code` VARCHAR(40) NULL,
    `resolution_note` VARCHAR(1000) NULL,
    `resolved_at` DATETIME(3) NULL,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `active_dedupe_key` VARCHAR(180) GENERATED ALWAYS AS (
        CASE WHEN `status` IN ('PENDING', 'ASSIGNED')
             THEN CONCAT(`reporter_user_id`, ':', `target_type`, ':', `target_id`, ':', `reason_code`)
             ELSE NULL END
    ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_active_dedupe` (`active_dedupe_key`),
    KEY `idx_report_queue` (`status`, `created_at`),
    KEY `idx_report_target` (`target_type`, `target_id`, `created_at`),
    KEY `idx_report_reporter` (`reporter_user_id`, `created_at`),
    KEY `idx_report_assignee` (`assignee_admin_id`, `status`, `created_at`),
    CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_report_target_type` CHECK (`target_type` IN ('ARTICLE', 'MOMENT', 'COMMENT', 'BLOG', 'USER', 'TEAM', 'CHAT')),
    CONSTRAINT `chk_report_status` CHECK (`status` IN ('PENDING', 'ASSIGNED', 'RESOLVED', 'DISMISSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Community reports';

CREATE TABLE `community_report_event` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `report_id` BIGINT UNSIGNED NOT NULL,
    `actor_type` VARCHAR(16) NOT NULL COMMENT 'USER or ADMIN',
    `actor_id` BIGINT NULL,
    `event_type` VARCHAR(32) NOT NULL COMMENT 'CREATED, CLAIMED, RESOLVED, DISMISSED',
    `before_snapshot` JSON NULL,
    `after_snapshot` JSON NULL,
    `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_report_event_report` (`report_id`, `occurred_at`),
    CONSTRAINT `fk_report_event_report` FOREIGN KEY (`report_id`) REFERENCES `community_report` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only report events';

DELIMITER $$
CREATE TRIGGER `community_report_event_prevent_update` BEFORE UPDATE ON `community_report_event`
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_report_event is append-only: UPDATE not allowed'; END$$
CREATE TRIGGER `community_report_event_prevent_delete` BEFORE DELETE ON `community_report_event`
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_report_event is append-only: DELETE not allowed'; END$$
DELIMITER ;

INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`) VALUES
    (9120, 9110, '举报中心', 2, '/community/reports', '/community/reports/index', 'community:report:list', 'FlagOutline', 20, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9121, 9120, '查询举报', 3, NULL, NULL, 'community:report:list', NULL, 1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9122, 9120, '处理举报', 3, NULL, NULL, 'community:report:handle', NULL, 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0)
ON DUPLICATE KEY UPDATE `permission`=VALUES(`permission`),`path`=VALUES(`path`),`component`=VALUES(`component`),`update_time`=CURRENT_TIMESTAMP;

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.id, values_to_grant.menu_id FROM `sys_role` r CROSS JOIN (SELECT 9120 AS menu_id UNION ALL SELECT 9121 UNION ALL SELECT 9122) values_to_grant
WHERE r.code='admin' AND r.deleted=0 AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` existing WHERE existing.role_id=r.id AND existing.menu_id=values_to_grant.menu_id);
