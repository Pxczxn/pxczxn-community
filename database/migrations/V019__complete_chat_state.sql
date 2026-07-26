SET NAMES utf8mb4;

ALTER TABLE `sys_chat_message`
    ADD COLUMN `sender_deleted` TINYINT NOT NULL DEFAULT 0
        COMMENT '发送方是否已清除该消息：0否 1是' AFTER `is_read`,
    ADD COLUMN `receiver_deleted` TINYINT NOT NULL DEFAULT 0
        COMMENT '接收方是否已清除该消息：0否 1是' AFTER `sender_deleted`,
    ADD INDEX `idx_chat_pair_time` (`sender_id`, `receiver_id`, `send_time`, `id`),
    ADD INDEX `idx_chat_receiver_unread` (`receiver_id`, `is_read`, `send_time`, `id`),
    ADD CONSTRAINT `chk_chat_message_type`
        CHECK (`msg_type` IN (1, 2, 3)),
    ADD CONSTRAINT `chk_chat_message_read`
        CHECK (`is_read` IN (0, 1)),
    ADD CONSTRAINT `chk_chat_message_visibility`
        CHECK (`sender_deleted` IN (0, 1) AND `receiver_deleted` IN (0, 1));

ALTER TABLE `sys_chat_group_member`
    ADD COLUMN `last_read_message_id` BIGINT NOT NULL DEFAULT 0
        COMMENT '该成员最后已读群消息ID' AFTER `muted`,
    ADD COLUMN `last_read_time` DATETIME NULL
        COMMENT '最近一次标记群消息已读时间' AFTER `last_read_message_id`,
    ADD CONSTRAINT `chk_chat_group_member_role`
        CHECK (`role` IN (0, 1, 2)),
    ADD CONSTRAINT `chk_chat_group_member_muted`
        CHECK (`muted` IN (0, 1));

ALTER TABLE `sys_chat_group_message`
    ADD INDEX `idx_chat_group_message_page` (`group_id`, `id`),
    ADD CONSTRAINT `chk_chat_group_message_type`
        CHECK (`msg_type` IN (1, 2, 3, 4));

-- Existing members should not receive a synthetic unread backlog merely
-- because read cursors were introduced.
UPDATE `sys_chat_group_member` `member`
LEFT JOIN (
    SELECT `group_id`, MAX(`id`) AS `latest_message_id`
    FROM `sys_chat_group_message`
    GROUP BY `group_id`
) `latest` ON `latest`.`group_id` = `member`.`group_id`
SET `member`.`last_read_message_id` = COALESCE(`latest`.`latest_message_id`, 0),
    `member`.`last_read_time` = CURRENT_TIMESTAMP;
