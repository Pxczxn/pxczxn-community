SET NAMES utf8mb4;

ALTER TABLE `team_invitation`
    DROP INDEX `uk_team_invitation_idempotency`,
    DROP COLUMN `idempotency_key`,
    DROP INDEX `uk_pending_invitation`,
    ADD UNIQUE KEY `uk_pending_invitation` (`team_id`, `invitee_user_id`, `role_code`, `is_pending`);
