SET NAMES utf8mb4;

ALTER TABLE `community_account_enforcement_appeal`
  ADD COLUMN IF NOT EXISTS `primary_reviewed_by_admin_id` BIGINT NULL AFTER `reviewed_at`,
  ADD COLUMN IF NOT EXISTS `primary_decision` VARCHAR(24) NULL AFTER `primary_reviewed_by_admin_id`,
  ADD COLUMN IF NOT EXISTS `primary_review_note` VARCHAR(2000) NULL AFTER `primary_decision`,
  ADD COLUMN IF NOT EXISTS `primary_reviewed_at` DATETIME(3) NULL AFTER `primary_review_note`,
  ADD COLUMN IF NOT EXISTS `final_reviewed_by_admin_id` BIGINT NULL AFTER `primary_reviewed_at`,
  ADD COLUMN IF NOT EXISTS `final_decision` VARCHAR(24) NULL AFTER `final_reviewed_by_admin_id`,
  ADD COLUMN IF NOT EXISTS `final_review_note` VARCHAR(2000) NULL AFTER `final_decision`,
  ADD COLUMN IF NOT EXISTS `final_reviewed_at` DATETIME(3) NULL AFTER `final_review_note`;

/* Existing values are retained. New writes use SUBMITTED, PRIMARY_REVIEWED and FINALIZED. */
UPDATE `sys_menu` SET `name`='账号管理', `update_time`=CURRENT_TIMESTAMP
WHERE `permission`='community:account:list' AND `deleted`=0;

