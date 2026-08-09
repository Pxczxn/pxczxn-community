/* M4-T006: durable, explainable anti-abuse windows. */
SET NAMES utf8mb4;
CREATE TABLE `community_abuse_window` (
  `actor_key` VARCHAR(160) NOT NULL,
  `action_type` VARCHAR(32) NOT NULL,
  `window_started_at` DATETIME(3) NOT NULL,
  `attempt_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `rejected_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`actor_key`,`action_type`),
  KEY `idx_abuse_window_updated` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `community_abuse_event` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `actor_key` VARCHAR(160) NOT NULL,
  `action_type` VARCHAR(32) NOT NULL,
  `decision` VARCHAR(16) NOT NULL,
  `threshold` INT UNSIGNED NOT NULL,
  `window_seconds` INT UNSIGNED NOT NULL,
  `attempt_count` INT UNSIGNED NOT NULL,
  `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`), KEY `idx_abuse_event_actor_time` (`actor_key`,`occurred_at`),
  CONSTRAINT `chk_abuse_event_decision` CHECK (`decision` IN ('ALLOW','REJECT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DELIMITER $$
CREATE TRIGGER `community_abuse_event_prevent_update` BEFORE UPDATE ON `community_abuse_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_abuse_event is append-only'; END$$
CREATE TRIGGER `community_abuse_event_prevent_delete` BEFORE DELETE ON `community_abuse_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_abuse_event is append-only'; END$$
DELIMITER ;
