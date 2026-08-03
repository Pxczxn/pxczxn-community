SET NAMES utf8mb4;

ALTER TABLE `community_account_enforcement_case`
  DROP CHECK `chk_account_enforcement_measure`,
  ADD CONSTRAINT `chk_account_enforcement_measure`
    CHECK (`measure_type` IN (
      'TEMP_FREEZE','LONG_FREEZE','DATA_CLEANUP','ACCOUNT_DELETE',
      'PASSWORD_RESET','SECURITY_LOGOUT','ACCOUNT_UNLOCK',
      'FREEZE_EXTEND','FREEZE_RELEASE','LOGIN_BLOCK','LOGIN_RESTORE',
      'ACCOUNT_DEACTIVATE','ACCOUNT_RESTORE'
    ));
