/*
 * Rollback: V023 M3 Team Foundation
 * Purpose: Remove all team domain tables and data.
 * Safety: Does NOT remove team blogs from the blog table or user data.
 */

SET NAMES utf8mb4;

-- Drop triggers before dropping table
DROP TRIGGER IF EXISTS `team_audit_event_prevent_delete`;
DROP TRIGGER IF EXISTS `team_audit_event_prevent_update`;

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS `team_audit_event`;
DROP TABLE IF EXISTS `team_permission`;
DROP TABLE IF EXISTS `team_role`;
DROP TABLE IF EXISTS `team_application`;
DROP TABLE IF EXISTS `team_invitation`;
DROP TABLE IF EXISTS `team_member`;
DROP TABLE IF EXISTS `team`;
