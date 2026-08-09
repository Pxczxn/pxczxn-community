/* M4-T002: per-user community block list and visibility controls. */
SET NAMES utf8mb4;

CREATE TABLE `community_block` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT 'Block ID',
    `blocker_user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Community user who created the block',
    `target_type` VARCHAR(16) NOT NULL COMMENT 'USER, BLOG, TAG, CHAT',
    `target_id` BIGINT UNSIGNED NOT NULL COMMENT 'Blocked resource or chat peer user ID',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_community_block_target` (`blocker_user_id`, `target_type`, `target_id`),
    KEY `idx_community_block_blocker` (`blocker_user_id`, `created_at`),
    KEY `idx_community_block_target` (`target_type`, `target_id`),
    CONSTRAINT `fk_community_block_blocker`
        FOREIGN KEY (`blocker_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_community_block_type`
        CHECK (`target_type` IN ('USER', 'BLOG', 'TAG', 'CHAT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Community user block list';
