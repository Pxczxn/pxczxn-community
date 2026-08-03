/* Account-level enforcement: evidence snapshots, staged approval, appeal and delayed deletion. */
SET NAMES utf8mb4;

CREATE TABLE `community_account_enforcement_case` (
  `id` BIGINT UNSIGNED NOT NULL,
  `target_user_id` BIGINT UNSIGNED NOT NULL,
  `measure_type` VARCHAR(24) NOT NULL COMMENT 'TEMP_FREEZE,LONG_FREEZE,DATA_CLEANUP,ACCOUNT_DELETE',
  `status` VARCHAR(32) NOT NULL COMMENT 'DRAFT,SUBMITTED,UNDER_REVIEW,APPROVED,PENDING_EXECUTION,ACTIVE,APPEAL_WINDOW,APPEALED,FINALIZED,REJECTED,REVOKED,EXECUTION_FAILED',
  `reason_code` VARCHAR(40) NOT NULL,
  `user_visible_reason` VARCHAR(1000) NOT NULL,
  `internal_reason` VARCHAR(2000) NOT NULL,
  `evidence_snapshot` JSON NOT NULL,
  `cleanup_scope` JSON NULL,
  `source_report_id` BIGINT UNSIGNED NULL,
  `requested_by_admin_id` BIGINT NULL,
  `requested_by_team_id` BIGINT UNSIGNED NULL,
  `requested_at` DATETIME(3) NOT NULL,
  `starts_at` DATETIME(3) NULL,
  `expires_at` DATETIME(3) NULL,
  `appeal_allowed` TINYINT(1) NOT NULL DEFAULT 1,
  `appeal_deadline_at` DATETIME(3) NULL,
  `execute_after` DATETIME(3) NULL,
  `finalized_at` DATETIME(3) NULL,
  `lock_version` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_account_enforcement_queue` (`status`,`measure_type`,`requested_at`),
  KEY `idx_account_enforcement_target` (`target_user_id`,`created_at`),
  CONSTRAINT `fk_account_enforcement_target` FOREIGN KEY (`target_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_account_enforcement_measure` CHECK (`measure_type` IN ('TEMP_FREEZE','LONG_FREEZE','DATA_CLEANUP','ACCOUNT_DELETE')),
  CONSTRAINT `chk_account_enforcement_status` CHECK (`status` IN ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','PENDING_EXECUTION','ACTIVE','APPEAL_WINDOW','APPEALED','FINALIZED','REJECTED','REVOKED','EXECUTION_FAILED')),
  CONSTRAINT `chk_account_enforcement_requester` CHECK ((`requested_by_admin_id` IS NOT NULL OR `requested_by_team_id` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号强制措施申请与延迟删除工作流';

CREATE TABLE `community_account_enforcement_review` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `case_id` BIGINT UNSIGNED NOT NULL,
  `stage` VARCHAR(24) NOT NULL COMMENT 'INITIAL,SECONDARY,FINAL',
  `reviewer_admin_id` BIGINT NOT NULL,
  `decision` VARCHAR(24) NOT NULL COMMENT 'APPROVE,REJECT,RETURN_FOR_EVIDENCE,REVOKE,UPHOLD',
  `review_note` VARCHAR(2000) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_account_enforcement_review_case` (`case_id`,`created_at`),
  UNIQUE KEY `uk_account_enforcement_reviewer_stage` (`case_id`,`stage`,`reviewer_admin_id`),
  CONSTRAINT `fk_account_enforcement_review_case` FOREIGN KEY (`case_id`) REFERENCES `community_account_enforcement_case` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号强制措施审核记录（仅追加）';

CREATE TABLE `community_account_enforcement_appeal` (
  `id` BIGINT UNSIGNED NOT NULL,
  `case_id` BIGINT UNSIGNED NOT NULL,
  `appellant_user_id` BIGINT UNSIGNED NOT NULL,
  `statement` VARCHAR(2000) NOT NULL,
  `evidence_snapshot` JSON NULL,
  `status` VARCHAR(24) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED,UNDER_REVIEW,UPHELD,MODIFIED,REVOKED,FINAL',
  `reviewed_by_admin_id` BIGINT NULL,
  `review_note` VARCHAR(2000) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `reviewed_at` DATETIME(3) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_account_enforcement_appeal_queue` (`status`,`created_at`),
  CONSTRAINT `fk_account_enforcement_appeal_case` FOREIGN KEY (`case_id`) REFERENCES `community_account_enforcement_case` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_account_enforcement_appeal_user` FOREIGN KEY (`appellant_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_account_enforcement_appeal_status` CHECK (`status` IN ('SUBMITTED','UNDER_REVIEW','UPHELD','MODIFIED','REVOKED','FINAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号强制措施申诉';

CREATE TABLE `community_account_enforcement_event` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `case_id` BIGINT UNSIGNED NOT NULL,
  `actor_type` VARCHAR(16) NOT NULL,
  `actor_id` BIGINT NULL,
  `event_type` VARCHAR(32) NOT NULL,
  `snapshot` JSON NULL,
  `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_account_enforcement_event_case` (`case_id`,`occurred_at`),
  CONSTRAINT `fk_account_enforcement_event_case` FOREIGN KEY (`case_id`) REFERENCES `community_account_enforcement_case` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号强制措施审计日志（仅追加）';

DELIMITER $$
CREATE TRIGGER `community_account_enforcement_review_prevent_update` BEFORE UPDATE ON `community_account_enforcement_review` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_account_enforcement_review is append-only: UPDATE not allowed'; END$$
CREATE TRIGGER `community_account_enforcement_review_prevent_delete` BEFORE DELETE ON `community_account_enforcement_review` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_account_enforcement_review is append-only: DELETE not allowed'; END$$
CREATE TRIGGER `community_account_enforcement_event_prevent_update` BEFORE UPDATE ON `community_account_enforcement_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_account_enforcement_event is append-only: UPDATE not allowed'; END$$
CREATE TRIGGER `community_account_enforcement_event_prevent_delete` BEFORE DELETE ON `community_account_enforcement_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_account_enforcement_event is append-only: DELETE not allowed'; END$$
DELIMITER ;
