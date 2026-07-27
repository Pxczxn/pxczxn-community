/*
 * Version: V023
 * Purpose: M3-T001 Team domain foundation - tables, roles, permissions, and audit events.
 * Depends on: V022 (menu structure) and existing blog/user tables.
 * Repeatable: Yes. Uses CREATE IF NOT EXISTS and INSERT IGNORE for idempotency.
 *
 * Creates team blogs (1:1 with blog), members with fixed roles (OWNER/ADMIN/EDITOR/AUTHOR),
 * invitations with hashed tokens, applications with approval workflow, and audit events.
 * Team roles are NOT in sys_role; they are business domain entities.
 */

SET NAMES utf8mb4;

-- Team table: 1:1 with TEAM type blog
CREATE TABLE IF NOT EXISTS `team` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Team ID (same as blog_id)',
    `blog_id` BIGINT NOT NULL COMMENT 'Unique foreign key to blog table',
    `owner_user_id` BIGINT NOT NULL COMMENT 'Current team owner',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISBANDED',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    `deleted_at` DATETIME NULL DEFAULT NULL COMMENT 'Soft delete timestamp',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_blog_id` (`blog_id`),
    KEY `idx_team_owner` (`owner_user_id`),
    KEY `idx_team_status` (`status`),
    CONSTRAINT `fk_team_blog` FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team blog metadata';

-- Team member table: team membership with roles
-- Fix NULL uniqueness: use generated column to enforce only one active member per team+user
CREATE TABLE IF NOT EXISTS `team_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Member record ID',
    `team_id` BIGINT NOT NULL COMMENT 'Team ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `role_code` VARCHAR(20) NOT NULL COMMENT 'OWNER/ADMIN/EDITOR/AUTHOR',
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Join timestamp',
    `left_at` DATETIME NULL DEFAULT NULL COMMENT 'Leave timestamp (null = active)',
    `invited_by_user_id` BIGINT NULL DEFAULT NULL COMMENT 'Inviter user ID',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `is_active` TINYINT AS (IF(left_at IS NULL, 1, NULL)) STORED COMMENT 'Generated: 1 if active, NULL if left',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_member_active` (`team_id`, `user_id`, `is_active`),
    KEY `idx_team_member_user` (`user_id`),
    KEY `idx_team_member_role` (`role_code`),
    KEY `idx_team_member_status` (`team_id`, `left_at`),
    CONSTRAINT `fk_team_member_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team membership records';

-- Team invitation table: token-based invitations
-- Prevent duplicate pending invitations for same team/user/role using generated column
CREATE TABLE IF NOT EXISTS `team_invitation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Invitation ID',
    `team_id` BIGINT NOT NULL COMMENT 'Team ID',
    `invitee_user_id` BIGINT NOT NULL COMMENT 'Invited user ID',
    `role_code` VARCHAR(20) NOT NULL COMMENT 'Target role',
    `token_hash` VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of random token',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/EXPIRED',
    `invited_by_user_id` BIGINT NOT NULL COMMENT 'Inviter user ID',
    `expires_at` DATETIME NOT NULL COMMENT 'Expiration time',
    `accepted_at` DATETIME NULL DEFAULT NULL COMMENT 'Acceptance timestamp',
    `rejected_at` DATETIME NULL DEFAULT NULL COMMENT 'Rejection timestamp',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `is_pending` TINYINT AS (IF(status = 'PENDING', 1, NULL)) STORED COMMENT 'Generated: 1 if pending, NULL otherwise',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invitation_token` (`token_hash`),
    UNIQUE KEY `uk_pending_invitation` (`team_id`, `invitee_user_id`, `role_code`, `is_pending`),
    KEY `idx_invitation_user` (`invitee_user_id`),
    KEY `idx_invitation_status` (`status`, `expires_at`),
    CONSTRAINT `fk_invitation_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team member invitations';

-- Team role table: fixed business roles
CREATE TABLE IF NOT EXISTS `team_role` (
    `role_code` VARCHAR(20) NOT NULL COMMENT 'Role code: OWNER/ADMIN/EDITOR/AUTHOR',
    `role_name` VARCHAR(50) NOT NULL COMMENT 'Display name',
    `description` VARCHAR(200) NULL DEFAULT NULL COMMENT 'Role description',
    `sort_order` INT NOT NULL COMMENT 'Display order (lower = higher authority)',
    `is_system` TINYINT NOT NULL DEFAULT 1 COMMENT 'System role flag (cannot delete)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team business roles';

-- Team permission table: role-permission mapping
CREATE TABLE IF NOT EXISTS `team_permission` (
    `role_code` VARCHAR(20) NOT NULL COMMENT 'Role code',
    `permission_code` VARCHAR(50) NOT NULL COMMENT 'Permission code',
    PRIMARY KEY (`role_code`, `permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team role permissions';

-- Team audit event table: append-only audit log
CREATE TABLE IF NOT EXISTS `team_audit_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Event ID',
    `team_id` BIGINT NOT NULL COMMENT 'Team ID',
    `actor_user_id` BIGINT NULL DEFAULT NULL COMMENT 'Actor user ID (null for system)',
    `event_type` VARCHAR(50) NOT NULL COMMENT 'Event type code',
    `target_type` VARCHAR(50) NULL DEFAULT NULL COMMENT 'Target entity type',
    `target_id` BIGINT NULL DEFAULT NULL COMMENT 'Target entity ID',
    `request_id` VARCHAR(64) NULL DEFAULT NULL COMMENT 'Request trace ID',
    `before_snapshot` JSON NULL DEFAULT NULL COMMENT 'State before change',
    `after_snapshot` JSON NULL DEFAULT NULL COMMENT 'State after change',
    `occurred_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Event timestamp',
    PRIMARY KEY (`id`),
    KEY `idx_audit_team` (`team_id`, `occurred_at`),
    KEY `idx_audit_actor` (`actor_user_id`),
    KEY `idx_audit_type` (`event_type`),
    CONSTRAINT `fk_audit_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team audit events (append-only)';

-- Trigger to prevent UPDATE on team_audit_event (append-only protection)
DROP TRIGGER IF EXISTS `team_audit_event_prevent_update`;
CREATE TRIGGER `team_audit_event_prevent_update`
BEFORE UPDATE ON `team_audit_event`
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'team_audit_event is append-only: UPDATE not allowed';
END;

-- Trigger to prevent DELETE on team_audit_event (append-only protection)
DROP TRIGGER IF EXISTS `team_audit_event_prevent_delete`;
CREATE TRIGGER `team_audit_event_prevent_delete`
BEFORE DELETE ON `team_audit_event`
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'team_audit_event is append-only: DELETE not allowed';
END;

-- Team application table: team blog creation requests
-- Add resource JSON and idempotency; enforce one pending application per user
CREATE TABLE IF NOT EXISTS `team_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Application ID',
    `applicant_user_id` BIGINT NOT NULL COMMENT 'Applicant user ID',
    `team_name` VARCHAR(100) NOT NULL COMMENT 'Requested team name',
    `team_slug` VARCHAR(100) NOT NULL COMMENT 'Requested team slug',
    `description` TEXT NULL DEFAULT NULL COMMENT 'Application description',
    `application_data` JSON NULL DEFAULT NULL COMMENT 'Additional application metadata',
    `idempotency_key` VARCHAR(64) NULL DEFAULT NULL COMMENT 'Client idempotency key',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    `reviewer_user_id` BIGINT NULL DEFAULT NULL COMMENT 'Reviewer user ID',
    `review_comment` TEXT NULL DEFAULT NULL COMMENT 'Review feedback',
    `reviewed_at` DATETIME NULL DEFAULT NULL COMMENT 'Review timestamp',
    `lock_version` INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submission time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    `is_pending` TINYINT AS (IF(status = 'PENDING', 1, NULL)) STORED COMMENT 'Generated: 1 if pending, NULL otherwise',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pending_application` (`applicant_user_id`, `is_pending`),
    UNIQUE KEY `uk_idempotency` (`idempotency_key`),
    KEY `idx_application_status` (`status`, `created_at`),
    KEY `idx_application_slug` (`team_slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team blog creation applications';

-- Insert fixed team roles
INSERT IGNORE INTO `team_role` (`role_code`, `role_name`, `description`, `sort_order`, `is_system`)
VALUES
    ('OWNER', 'Owner', 'Team owner with full control', 1, 1),
    ('ADMIN', 'Administrator', 'Can manage members except OWNER/ADMIN', 2, 1),
    ('EDITOR', 'Editor', 'Can edit all team content but not manage members', 3, 1),
    ('AUTHOR', 'Author', 'Can only edit own team articles', 4, 1);

-- Insert role-permission mappings
INSERT IGNORE INTO `team_permission` (`role_code`, `permission_code`)
VALUES
    -- OWNER: all permissions
    ('OWNER', 'MANAGE_TEAM'),
    ('OWNER', 'MANAGE_MEMBERS'),
    ('OWNER', 'TRANSFER_OWNERSHIP'),
    ('OWNER', 'DISBAND_TEAM'),
    ('OWNER', 'EDIT_ALL_ARTICLES'),
    ('OWNER', 'DELETE_ALL_ARTICLES'),
    ('OWNER', 'MANAGE_SUBMISSIONS'),
    ('OWNER', 'MANAGE_SERIES'),
    ('OWNER', 'VIEW_AUDIT'),
    -- ADMIN: cannot manage OWNER/ADMIN or transfer ownership
    ('ADMIN', 'MANAGE_TEAM'),
    ('ADMIN', 'MANAGE_MEMBERS'),
    ('ADMIN', 'EDIT_ALL_ARTICLES'),
    ('ADMIN', 'DELETE_ALL_ARTICLES'),
    ('ADMIN', 'MANAGE_SUBMISSIONS'),
    ('ADMIN', 'MANAGE_SERIES'),
    ('ADMIN', 'VIEW_AUDIT'),
    -- EDITOR: content only, no member management
    ('EDITOR', 'EDIT_ALL_ARTICLES'),
    ('EDITOR', 'DELETE_ALL_ARTICLES'),
    ('EDITOR', 'MANAGE_SUBMISSIONS'),
    ('EDITOR', 'MANAGE_SERIES'),
    -- AUTHOR: own articles only
    ('AUTHOR', 'EDIT_OWN_ARTICLES'),
    ('AUTHOR', 'DELETE_OWN_ARTICLES');
