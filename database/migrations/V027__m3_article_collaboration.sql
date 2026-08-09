/* M3-T006: article-scoped collaboration invitations, credits, and audit. */
SET NAMES utf8mb4;

CREATE TABLE `article_collaboration_invitation` (
    `id` BIGINT UNSIGNED NOT NULL,
    `article_id` BIGINT UNSIGNED NOT NULL,
    `invitee_user_id` BIGINT UNSIGNED NOT NULL,
    `invited_by_user_id` BIGINT UNSIGNED NOT NULL,
    `contribution_type` VARCHAR(24) NOT NULL,
    `can_edit` TINYINT NOT NULL DEFAULT 0,
    `attribution_order` SMALLINT UNSIGNED NOT NULL,
    `message` VARCHAR(1000) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `idempotency_key` VARCHAR(80) NOT NULL,
    `expires_at` DATETIME(3) NOT NULL,
    `responded_at` DATETIME(3) NULL,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `active_invitee_user_id` BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN `status` = 'PENDING' THEN `invitee_user_id` ELSE NULL END
    ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_collab_invitation_idempotency` (`idempotency_key`),
    UNIQUE KEY `uk_article_collab_active_invitee` (`article_id`, `active_invitee_user_id`),
    KEY `idx_article_collab_invitee` (`invitee_user_id`, `status`, `created_at`),
    KEY `idx_article_collab_article` (`article_id`, `status`, `created_at`),
    CONSTRAINT `fk_article_collab_invitation_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_article_collab_invitation_invitee` FOREIGN KEY (`invitee_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_article_collab_invitation_inviter` FOREIGN KEY (`invited_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_article_collab_invitation_type` CHECK (`contribution_type` IN ('CO_AUTHOR', 'RESEARCH', 'REVIEW', 'ILLUSTRATION')),
    CONSTRAINT `chk_article_collab_invitation_status` CHECK (`status` IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT `chk_article_collab_invitation_edit` CHECK (`can_edit` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Article-scoped collaboration invitations';

CREATE TABLE `article_collaborator` (
    `id` BIGINT UNSIGNED NOT NULL,
    `article_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `invitation_id` BIGINT UNSIGNED NOT NULL,
    `contribution_type` VARCHAR(24) NOT NULL,
    `can_edit` TINYINT NOT NULL DEFAULT 0,
    `attribution_order` SMALLINT UNSIGNED NOT NULL,
    `accepted_at` DATETIME(3) NOT NULL,
    `revoked_at` DATETIME(3) NULL,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `active_user_id` BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN `revoked_at` IS NULL THEN `user_id` ELSE NULL END
    ) STORED,
    `active_attribution_order` SMALLINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN `revoked_at` IS NULL THEN `attribution_order` ELSE NULL END
    ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_collaborator_active_user` (`article_id`, `active_user_id`),
    UNIQUE KEY `uk_article_collaborator_invitation` (`invitation_id`),
    UNIQUE KEY `uk_article_collaborator_active_order` (`article_id`, `active_attribution_order`),
    KEY `idx_article_collaborator_article` (`article_id`, `attribution_order`),
    KEY `idx_article_collaborator_user` (`user_id`, `revoked_at`),
    CONSTRAINT `fk_article_collaborator_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_article_collaborator_user` FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_article_collaborator_invitation` FOREIGN KEY (`invitation_id`) REFERENCES `article_collaboration_invitation` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_article_collaborator_type` CHECK (`contribution_type` IN ('CO_AUTHOR', 'RESEARCH', 'REVIEW', 'ILLUSTRATION')),
    CONSTRAINT `chk_article_collaborator_edit` CHECK (`can_edit` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Accepted article collaborators and public credits';

CREATE TABLE `article_collaboration_audit_event` (
    `id` BIGINT UNSIGNED NOT NULL,
    `article_id` BIGINT UNSIGNED NOT NULL,
    `actor_user_id` BIGINT UNSIGNED NULL,
    `event_type` VARCHAR(48) NOT NULL,
    `target_user_id` BIGINT UNSIGNED NULL,
    `before_snapshot` JSON NULL,
    `after_snapshot` JSON NULL,
    `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_article_collab_audit_article` (`article_id`, `occurred_at`),
    CONSTRAINT `fk_article_collab_audit_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only article collaboration audit';
