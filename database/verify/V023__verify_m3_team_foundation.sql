SET NAMES utf8mb4;

-- Verify all 7 tables exist
SELECT IF(
    COUNT(*) = 7,
    'PASS',
    'FAIL'
) AS `team_tables_created`
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('team', 'team_member', 'team_invitation', 'team_role', 'team_permission', 'team_audit_event', 'team_application');

-- Verify unique constraint on team.blog_id
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_blog_id_unique_constraint`
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team'
  AND CONSTRAINT_TYPE = 'UNIQUE'
  AND CONSTRAINT_NAME = 'uk_team_blog_id';

-- Verify foreign key from team.blog_id to blog.id
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_blog_foreign_key`
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  AND CONSTRAINT_NAME = 'fk_team_blog';

-- Verify team_member has is_active generated column
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_member_is_active_column`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_member'
  AND COLUMN_NAME = 'is_active'
  AND GENERATION_EXPRESSION IS NOT NULL;

-- Verify team_member unique constraint for active members (using is_active)
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_member_active_unique_constraint`
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_member'
  AND CONSTRAINT_TYPE = 'UNIQUE'
  AND CONSTRAINT_NAME = 'uk_team_member_active';

-- Verify team_member foreign key
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_member_foreign_key`
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_member'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  AND CONSTRAINT_NAME = 'fk_team_member_team';

-- Verify team_invitation has is_pending generated column
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_invitation_is_pending_column`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_invitation'
  AND COLUMN_NAME = 'is_pending'
  AND GENERATION_EXPRESSION IS NOT NULL;

-- Verify team_invitation unique constraint for pending invitations (using is_pending)
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_invitation_pending_unique_constraint`
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_invitation'
  AND CONSTRAINT_TYPE = 'UNIQUE'
  AND CONSTRAINT_NAME = 'uk_pending_invitation';

-- Verify team_invitation foreign key
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_invitation_foreign_key`
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_invitation'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  AND CONSTRAINT_NAME = 'fk_invitation_team';

-- Verify team_application has application_data JSON column
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_application_data_column`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_application'
  AND COLUMN_NAME = 'application_data'
  AND DATA_TYPE = 'json';

-- Verify team_application has idempotency_key column
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_application_idempotency_key_column`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_application'
  AND COLUMN_NAME = 'idempotency_key';

-- Verify team_application has is_pending generated column
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_application_is_pending_column`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_application'
  AND COLUMN_NAME = 'is_pending'
  AND GENERATION_EXPRESSION IS NOT NULL;

-- Verify team_application unique pending constraint (using is_pending)
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_application_pending_unique_constraint`
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_application'
  AND CONSTRAINT_TYPE = 'UNIQUE'
  AND CONSTRAINT_NAME = 'uk_pending_application';

-- Verify team_application idempotency unique constraint
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_application_idempotency_unique_constraint`
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_application'
  AND CONSTRAINT_TYPE = 'UNIQUE'
  AND CONSTRAINT_NAME = 'uk_idempotency';

-- Verify team_audit_event foreign key
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_audit_event_foreign_key`
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'team_audit_event'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  AND CONSTRAINT_NAME = 'fk_audit_team';

-- Verify 4 fixed team roles exist
SELECT IF(
    COUNT(*) = 4,
    'PASS',
    'FAIL'
) AS `team_roles_seeded`
FROM `team_role`
WHERE `role_code` IN ('OWNER', 'ADMIN', 'EDITOR', 'AUTHOR')
  AND `is_system` = 1;

-- Verify role permissions are configured
SELECT IF(
    COUNT(*) >= 20,
    'PASS',
    'FAIL'
) AS `team_permissions_seeded`
FROM `team_permission`
WHERE `role_code` IN ('OWNER', 'ADMIN', 'EDITOR', 'AUTHOR');

-- Verify OWNER has TRANSFER_OWNERSHIP permission (unique to owner)
SELECT IF(
    COUNT(*) = 1,
    'PASS',
    'FAIL'
) AS `owner_transfer_permission_exclusive`
FROM `team_permission`
WHERE `permission_code` = 'TRANSFER_OWNERSHIP'
  AND `role_code` = 'OWNER';

-- Verify role-permission mapping integrity (ADMIN cannot have TRANSFER_OWNERSHIP)
SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `admin_cannot_transfer_ownership`
FROM `team_permission`
WHERE `permission_code` = 'TRANSFER_OWNERSHIP'
  AND `role_code` = 'ADMIN';

-- Verify EDITOR cannot manage members
SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `editor_cannot_manage_members`
FROM `team_permission`
WHERE `permission_code` = 'MANAGE_MEMBERS'
  AND `role_code` = 'EDITOR';

-- Verify AUTHOR only has own-article permissions
SELECT IF(
    COUNT(*) = 0,
    'PASS',
    'FAIL'
) AS `author_cannot_edit_all_articles`
FROM `team_permission`
WHERE `permission_code` IN ('EDIT_ALL_ARTICLES', 'DELETE_ALL_ARTICLES', 'MANAGE_MEMBERS', 'MANAGE_TEAM')
  AND `role_code` = 'AUTHOR';

-- Verify team_audit_event has UPDATE prevention trigger
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_audit_event_update_trigger`
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = DATABASE()
  AND EVENT_OBJECT_TABLE = 'team_audit_event'
  AND TRIGGER_NAME = 'team_audit_event_prevent_update'
  AND EVENT_MANIPULATION = 'UPDATE';

-- Verify team_audit_event has DELETE prevention trigger
SELECT IF(
    COUNT(*) >= 1,
    'PASS',
    'FAIL'
) AS `team_audit_event_delete_trigger`
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = DATABASE()
  AND EVENT_OBJECT_TABLE = 'team_audit_event'
  AND TRIGGER_NAME = 'team_audit_event_prevent_delete'
  AND EVENT_MANIPULATION = 'DELETE';
