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
