/* M4-T003: appeals against resolved community reports. */
SET NAMES utf8mb4;

CREATE TABLE `community_appeal` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT 'Appeal ID',
    `report_id` BIGINT UNSIGNED NOT NULL COMMENT 'Resolved report being appealed',
    `appellant_user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Owner of the reported target',
    `appeal_reason` VARCHAR(1000) NOT NULL,
    `evidence_json` JSON NULL,
    `status` VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    `reviewer_admin_id` BIGINT NULL,
    `review_note` VARCHAR(1000) NULL,
    `reviewed_at` DATETIME(3) NULL,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_community_appeal_report_appellant` (`report_id`, `appellant_user_id`),
    KEY `idx_community_appeal_queue` (`status`, `created_at`),
    KEY `idx_community_appeal_appellant` (`appellant_user_id`, `created_at`),
    CONSTRAINT `fk_community_appeal_report` FOREIGN KEY (`report_id`) REFERENCES `community_report` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_community_appeal_appellant` FOREIGN KEY (`appellant_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_community_appeal_status` CHECK (`status` IN ('PENDING', 'UPHELD', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Appeals against report decisions';

CREATE TABLE `community_appeal_event` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `appeal_id` BIGINT UNSIGNED NOT NULL,
    `actor_type` VARCHAR(16) NOT NULL COMMENT 'USER or ADMIN',
    `actor_id` BIGINT NULL,
    `event_type` VARCHAR(32) NOT NULL COMMENT 'CREATED, UPHELD, REVOKED',
    `before_snapshot` JSON NULL,
    `after_snapshot` JSON NULL,
    `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_community_appeal_event_appeal` (`appeal_id`, `occurred_at`),
    CONSTRAINT `fk_community_appeal_event_appeal` FOREIGN KEY (`appeal_id`) REFERENCES `community_appeal` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only appeal events';

DELIMITER $$
CREATE TRIGGER `community_appeal_event_prevent_update` BEFORE UPDATE ON `community_appeal_event`
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_appeal_event is append-only: UPDATE not allowed'; END$$
CREATE TRIGGER `community_appeal_event_prevent_delete` BEFORE DELETE ON `community_appeal_event`
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_appeal_event is append-only: DELETE not allowed'; END$$
DELIMITER ;

INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`) VALUES
    (9130, 9110, '申诉中心', 2, '/community/appeals', '/community/appeals/index', 'community:appeal:list', 'ShieldCheck', 21, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9131, 9130, '查询申诉', 3, NULL, NULL, 'community:appeal:list', NULL, 1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (9132, 9130, '复核申诉', 3, NULL, NULL, 'community:appeal:handle', NULL, 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0)
ON DUPLICATE KEY UPDATE `permission`=VALUES(`permission`),`path`=VALUES(`path`),`component`=VALUES(`component`),`update_time`=CURRENT_TIMESTAMP;

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.id, values_to_grant.menu_id FROM `sys_role` r CROSS JOIN (SELECT 9130 AS menu_id UNION ALL SELECT 9131 UNION ALL SELECT 9132) values_to_grant
WHERE r.code='admin' AND r.deleted=0 AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` existing WHERE existing.role_id=r.id AND existing.menu_id=values_to_grant.menu_id);
