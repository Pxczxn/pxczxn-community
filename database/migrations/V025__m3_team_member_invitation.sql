/* M3-T003: durable idempotency for team invitations. */
SET NAMES utf8mb4;

ALTER TABLE `team_invitation`
    ADD COLUMN `idempotency_key` VARCHAR(64) NULL COMMENT 'Client idempotency key',
    ADD UNIQUE KEY `uk_team_invitation_idempotency` (`idempotency_key`),
    DROP INDEX `uk_pending_invitation`,
    ADD UNIQUE KEY `uk_pending_invitation` (`team_id`, `invitee_user_id`, `is_pending`);
