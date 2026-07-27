/* M3-T005: immutable external article submissions and two-stage review. */
SET NAMES utf8mb4;

CREATE TABLE `team_submission` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT 'Submission ID',
    `source_article_id` BIGINT UNSIGNED NOT NULL COMMENT 'Personal source article; never reassigned',
    `fixed_source_version_id` BIGINT UNSIGNED NOT NULL COMMENT 'Immutable source snapshot',
    `target_team_id` BIGINT UNSIGNED NOT NULL COMMENT 'Destination team',
    `submitted_by_user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Source author',
    `supersedes_submission_id` BIGINT UNSIGNED NULL COMMENT 'Previous revision/rejection submission',
    `status` VARCHAR(32) NOT NULL DEFAULT 'TEAM_PENDING' COMMENT 'Two-stage review state',
    `team_reviewer_user_id` BIGINT UNSIGNED NULL,
    `team_review_comment` VARCHAR(1000) NULL,
    `team_reviewed_at` DATETIME(3) NULL,
    `platform_reviewer_admin_id` BIGINT NULL,
    `platform_review_comment` VARCHAR(1000) NULL,
    `platform_reviewed_at` DATETIME(3) NULL,
    `published_team_article_id` BIGINT UNSIGNED NULL COMMENT 'Independent published team article',
    `idempotency_key` VARCHAR(80) NOT NULL,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `active_source_target` VARCHAR(80) GENERATED ALWAYS AS (
        CASE WHEN `status` IN ('TEAM_PENDING', 'PLATFORM_PENDING', 'PLATFORM_PUBLISHING')
             THEN CONCAT(`source_article_id`, ':', `target_team_id`) ELSE NULL END
    ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_submission_idempotency` (`idempotency_key`),
    UNIQUE KEY `uk_team_submission_active_source_target` (`active_source_target`),
    UNIQUE KEY `uk_team_submission_published_article` (`published_team_article_id`),
    KEY `idx_team_submission_team_queue` (`target_team_id`, `status`, `created_at`),
    KEY `idx_team_submission_author` (`submitted_by_user_id`, `created_at`),
    KEY `idx_team_submission_source` (`source_article_id`, `created_at`),
    CONSTRAINT `fk_team_submission_source_article`
        FOREIGN KEY (`source_article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_team_submission_fixed_version`
        FOREIGN KEY (`fixed_source_version_id`) REFERENCES `article_version` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_team_submission_team`
        FOREIGN KEY (`target_team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_team_submission_author`
        FOREIGN KEY (`submitted_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_team_submission_supersedes`
        FOREIGN KEY (`supersedes_submission_id`) REFERENCES `team_submission` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_team_submission_published_article`
        FOREIGN KEY (`published_team_article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_team_submission_status` CHECK (`status` IN (
        'TEAM_PENDING', 'TEAM_REVISION_REQUIRED', 'TEAM_REJECTED',
        'PLATFORM_PENDING', 'PLATFORM_PUBLISHING', 'PLATFORM_REVISION_REQUIRED',
        'PLATFORM_REJECTED', 'PUBLISHED'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='External personal-article submissions for team publication';
