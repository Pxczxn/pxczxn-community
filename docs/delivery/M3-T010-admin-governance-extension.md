# M3-T010 Admin Governance Extension

This delivery completes the remaining canonical M3-T009 admin extension scope that was not already covered by the team-application, team-submission, and series-review deliveries.

## Delivered

- `GET /admin-api/community/teams` provides paginated team governance data with lifecycle status, owner, blog identity, and active-member count.
- `GET /admin-api/community/teams/{teamId}` provides the active member relationship and the 50 most recent immutable team audit events.
- `GET /admin-api/community/collaborations` provides paginated article collaboration relationships, including collaborator identity, contribution type, attribution order, edit entitlement, and active or revoked state.
- All team governance reads require `community:team:list`; collaboration reads require the existing `community:article:list` permission.
- `/community/teams` no longer renders a planning placeholder. It provides team and collaboration tabs, team detail, member and audit-event tables, loading, empty, and error states using the real admin API.

## Validation

- Backend compile for `pxczxn-biz` and `pxczxn-admin-api` with required reactor modules passed.
- Admin `typecheck`, `lint`, source-contract tests, and production build passed. Lint retains only pre-existing warnings outside this task.
- The source-contract test confirms the route uses the real teams view and that the teams and collaborations API clients remain registered.

## M3 Boundary

This is a read-only platform-governance surface. Team lifecycle writes remain in their respective audited workflows, and collaboration changes remain with the article author and invitation workflow. The cross-role, fixed-version, and acceptance/rejection browser journeys remain part of M3-T010 full acceptance.
