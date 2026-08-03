SET NAMES utf8mb4;

ALTER TABLE `community_chat_message`
  ADD COLUMN `sender_deleted_at` DATETIME(3) NULL AFTER `deleted_at`,
  ADD COLUMN `recipient_deleted_at` DATETIME(3) NULL AFTER `sender_deleted_at`;

CREATE INDEX `idx_community_chat_sender_visibility`
  ON `community_chat_message` (`sender_user_id`, `sender_deleted_at`, `id`);

CREATE INDEX `idx_community_chat_recipient_visibility`
  ON `community_chat_message` (`recipient_user_id`, `recipient_deleted_at`, `id`);
