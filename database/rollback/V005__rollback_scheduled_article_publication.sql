/*
 * Removes only V005 scheduling infrastructure.
 * Existing article rows and published content are preserved.
 */

SET NAMES utf8mb4;

DELETE FROM `sys_job`
WHERE `invoke_target` = 'articleScheduledPublishTask.runDueBatch';

DROP TABLE IF EXISTS `article_publish_task`;
