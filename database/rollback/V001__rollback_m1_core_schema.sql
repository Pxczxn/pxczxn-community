/*
 * 高风险回滚：会永久删除 M1 社区业务表及其中全部数据。
 *
 * 默认拒绝执行。仅在已完成备份并确认目标数据库后，在同一 MySQL 会话先执行：
 *
 *   SET @PXCZXN_CONFIRM_V001_ROLLBACK = 1;
 *
 * 然后再执行本文件。
 */

DELIMITER //
DROP PROCEDURE IF EXISTS `rollback_pxczxn_v001`//
CREATE PROCEDURE `rollback_pxczxn_v001`()
BEGIN
    IF COALESCE(@PXCZXN_CONFIRM_V001_ROLLBACK, 0) <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Refusing destructive V001 rollback without explicit confirmation';
    END IF;

    SET FOREIGN_KEY_CHECKS = 0;
    DROP TABLE IF EXISTS `community_file_reference`;
    DROP TABLE IF EXISTS `file_object`;
    DROP TABLE IF EXISTS `community_notification_recipient`;
    DROP TABLE IF EXISTS `community_notification`;
    DROP TABLE IF EXISTS `content_review_task`;
    DROP TABLE IF EXISTS `article_tag`;
    DROP TABLE IF EXISTS `platform_tag`;
    DROP TABLE IF EXISTS `article_version`;
    DROP TABLE IF EXISTS `article`;
    DROP TABLE IF EXISTS `blog_category`;
    DROP TABLE IF EXISTS `blog_setting`;
    DROP TABLE IF EXISTS `blog`;
    DROP TABLE IF EXISTS `community_user_preference`;
    DROP TABLE IF EXISTS `community_user_login_account`;
    DROP TABLE IF EXISTS `community_user`;
    SET FOREIGN_KEY_CHECKS = 1;
END//
DELIMITER ;

CALL `rollback_pxczxn_v001`();
DROP PROCEDURE `rollback_pxczxn_v001`;
