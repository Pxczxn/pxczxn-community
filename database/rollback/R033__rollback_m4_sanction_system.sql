DROP TRIGGER IF EXISTS `community_sanction_event_prevent_delete`;
DROP TRIGGER IF EXISTS `community_sanction_event_prevent_update`;
ALTER TABLE `community_user` DROP COLUMN IF EXISTS `sanction_original_status`, DROP COLUMN IF EXISTS `submission_restricted_until`;
DROP TABLE IF EXISTS `community_sanction_rate_limit`;
DROP TABLE IF EXISTS `community_sanction_event`;
DROP TABLE IF EXISTS `community_sanction`;
