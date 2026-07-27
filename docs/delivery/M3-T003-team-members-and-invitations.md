# M3-T003 Team Members and Invitations

Status: complete

- Implements invitations, accept/reject, leave, removal, role changes, ownership transfer, and disbanding.
- Invitation safety includes a SHA-256 random token hash, idempotency key, one pending invitation per team/user, expiration persistence, optimistic state transitions, audit events, and after-commit notifications.
- Only OWNER and ADMIN can delete team articles. EDITOR and AUTHOR are denied.
- The blog client adds `/team-invitations` with loading, empty, error, accept, and reject states.
- V023 fixes unsigned foreign-key compatibility and trigger delimiters. V024/V025 use MySQL 8.0-compatible migration and rollback syntax.

Verification:

```text
Backend full Maven test: PASS
Focused team authority/member tests: 30 passed
Web typecheck/lint/test/build: PASS/PASS/5 passed/PASS
Admin typecheck/lint/test/build: PASS/PASS with existing warnings/PASS/PASS
Isolated MySQL 8.0.46 migration and V023-V025 verify: PASS
```
