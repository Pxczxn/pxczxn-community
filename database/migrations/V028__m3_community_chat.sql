SET NAMES utf8mb4;

CREATE TABLE `community_chat_message` (
    `id` BIGINT UNSIGNED NOT NULL,
    `sender_user_id` BIGINT UNSIGNED NOT NULL,
    `recipient_user_id` BIGINT UNSIGNED NOT NULL,
    `content_text` VARCHAR(2000) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'SENT',
    `read_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `deleted_at` DATETIME(3) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_community_chat_recipient` (`recipient_user_id`, `status`, `id`),
    KEY `idx_community_chat_pair` (`sender_user_id`, `recipient_user_id`, `id`),
    CONSTRAINT `fk_community_chat_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_community_chat_recipient` FOREIGN KEY (`recipient_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_community_chat_status` CHECK (`status` IN ('SENT', 'READ', 'DELETED')),
    CONSTRAINT `chk_community_chat_distinct_users` CHECK (`sender_user_id` <> `recipient_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Community user private chat messages';
