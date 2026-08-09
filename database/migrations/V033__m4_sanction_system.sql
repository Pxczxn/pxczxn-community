/* M4-T004: enforceable user sanctions with immutable history. */
SET NAMES utf8mb4;

CREATE TABLE `community_sanction` (
    `id` BIGINT UNSIGNED NOT NULL,
    `target_user_id` BIGINT UNSIGNED NOT NULL,
    `sanction_type` VARCHAR(32) NOT NULL COMMENT 'WARNING,RATE_LIMIT,COMMENT_BAN,MOMENT_BAN,SUBMISSION_BAN,PUBLISH_SUSPEND,LOGIN_SUSPEND,PERMANENT_BAN',
    `reason_code` VARCHAR(40) NOT NULL,
    `reason_note` VARCHAR(1000) NULL,
    `source_report_id` BIGINT UNSIGNED NULL,
    `issued_by_admin_id` BIGINT NOT NULL,
    `starts_at` DATETIME(3) NOT NULL,
    `expires_at` DATETIME(3) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `revoked_by_admin_id` BIGINT NULL,
    `revoked_at` DATETIME(3) NULL,
    `revoke_note` VARCHAR(1000) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_sanction_target_active` (`target_user_id`,`status`,`expires_at`),
    KEY `idx_sanction_report` (`source_report_id`),
    CONSTRAINT `fk_sanction_target` FOREIGN KEY (`target_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_sanction_type` CHECK (`sanction_type` IN ('WARNING','RATE_LIMIT','COMMENT_BAN','MOMENT_BAN','SUBMISSION_BAN','PUBLISH_SUSPEND','LOGIN_SUSPEND','PERMANENT_BAN')),
    CONSTRAINT `chk_sanction_status` CHECK (`status` IN ('ACTIVE','EXPIRED','REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `community_sanction_event` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, `sanction_id` BIGINT UNSIGNED NOT NULL, `actor_type` VARCHAR(16) NOT NULL, `actor_id` BIGINT NULL, `event_type` VARCHAR(32) NOT NULL, `snapshot` JSON NULL, `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`), KEY `idx_sanction_event` (`sanction_id`,`occurred_at`), CONSTRAINT `fk_sanction_event` FOREIGN KEY (`sanction_id`) REFERENCES `community_sanction` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `community_sanction_rate_limit` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `action_type` VARCHAR(24) NOT NULL,
    `last_action_at` DATETIME(3) NOT NULL,
    PRIMARY KEY (`user_id`, `action_type`),
    CONSTRAINT `fk_sanction_rate_limit_user` FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `community_user`
    ADD COLUMN `submission_restricted_until` DATETIME(3) NULL COMMENT '处罚导致的投稿限制截止时间',
    ADD COLUMN `sanction_original_status` VARCHAR(24) NULL COMMENT '首次登录类处罚前的账号状态';

DELIMITER $$
CREATE TRIGGER `community_sanction_event_prevent_update` BEFORE UPDATE ON `community_sanction_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_sanction_event is append-only: UPDATE not allowed'; END$$
CREATE TRIGGER `community_sanction_event_prevent_delete` BEFORE DELETE ON `community_sanction_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_sanction_event is append-only: DELETE not allowed'; END$$
DELIMITER ;

INSERT INTO `sys_menu` (`id`,`parent_id`,`name`,`type`,`path`,`component`,`permission`,`icon`,`sort`,`visible`,`status`,`is_frame`,`create_time`,`update_time`,`create_by`,`update_by`,`deleted`) VALUES
 (9140,9110,'处罚体系',2,'/community/sanctions','/community/sanctions/index','community:sanction:list','HammerOutline',22,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9141,9140,'查询处罚',3,NULL,NULL,'community:sanction:list',NULL,1,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0),
 (9142,9140,'执行处罚',3,NULL,NULL,'community:sanction:handle',NULL,2,1,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0)
ON DUPLICATE KEY UPDATE `permission`=VALUES(`permission`),`path`=VALUES(`path`),`component`=VALUES(`component`),`update_time`=CURRENT_TIMESTAMP;
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) SELECT r.id,v.menu_id FROM `sys_role` r CROSS JOIN (SELECT 9140 menu_id UNION ALL SELECT 9141 UNION ALL SELECT 9142) v WHERE r.code='admin' AND r.deleted=0 AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` e WHERE e.role_id=r.id AND e.menu_id=v.menu_id);
