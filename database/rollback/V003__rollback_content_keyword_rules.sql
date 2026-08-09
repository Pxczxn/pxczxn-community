/*
 * High-risk rollback: permanently removes all configured keyword rules.
 *
 * Refused by default. After a backup and target-database check, run in the
 * same MySQL session:
 *
 *   SET @PXCZXN_CONFIRM_V003_ROLLBACK = 1;
 */

DELIMITER //
DROP PROCEDURE IF EXISTS `rollback_pxczxn_v003`//
CREATE PROCEDURE `rollback_pxczxn_v003`()
BEGIN
    IF COALESCE(@PXCZXN_CONFIRM_V003_ROLLBACK, 0) <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Refusing destructive V003 rollback without explicit confirmation';
    END IF;

    DROP TABLE IF EXISTS `content_keyword_rule`;
END//
DELIMITER ;

CALL `rollback_pxczxn_v003`();
DROP PROCEDURE `rollback_pxczxn_v003`;
