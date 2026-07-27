# M3-T009 Series And Serialization

This delivery closes the canonical M3-T007 "Series And Serialization" requirement. The local execution sequence labels it M3-T009 because the earlier M3 worktree sequence also includes community chat and homepage discovery.

## Delivered

- Team managers can create and update draft series, choose a serialization state, arrange team articles as chapters, and submit a complete series for platform review.
- A chapter belongs to at most one series. Chapter order is unique within a series and is preserved by the public detail API.
- Only published chapters appear in public data. A series cannot be submitted until it has at least one published chapter.
- Platform reviewers can list pending series and approve or reject them with an audit comment.
- The public web has real list and detail routes. The team workspace exposes series management only when the server grants the `SERIES` capability.

## API

- `GET /api/v1/series`
- `GET /api/v1/series/{seriesId}`
- `GET|POST /api/v1/teams/{teamId}/series`
- `GET /api/v1/teams/{teamId}/series/articles`
- `PATCH /api/v1/series/{seriesId}`
- `POST /api/v1/series/{seriesId}/chapters`
- `POST /api/v1/series/{seriesId}/submit-review`
- `GET /admin-api/community/series`
- `POST /admin-api/community/series/{seriesId}/approve`
- `POST /admin-api/community/series/{seriesId}/reject`

## Database Evidence

`V029__m3_team_series.sql` was applied to the isolated MySQL database `pxczxn_m3_t009_runtime`.

- The verification script confirmed both series tables, five required constraints/indexes, and the `community:series:review` permission.
- `R029__rollback_m3_team_series.sql` removed the schema objects successfully.
- Applying the migration and verification again succeeded.

The repository's historical migration chain cannot currently bootstrap an empty database because its early baseline assumes pre-existing scaffold tables and its V001 checksum differs from the retained M3 baseline. This does not affect the direct V029 migration, verify, rollback, and reapply validation above.

## Validation

- Focused backend tests passed: `TeamSeriesServiceImplTest` (4 tests) and `CommunityPublicRoutePolicyTest` (2 tests).
- Full backend Maven test suite and full executable-jar package passed.
- Web `typecheck`, `lint`, `test`, and production `build` passed.
- Admin `typecheck`, `lint`, `test`, and production `build` passed. The lint run retains pre-existing warnings outside this task but has no errors.
- Browser and HTTP checks confirmed the public empty state, public 404 response, unauthenticated team/admin 401 responses, and credentialed CORS from `http://localhost:8848` to `http://localhost:8860`.

## Remaining Runtime Test Boundary

The create, chapter-order, submit, and approve write path has unit coverage, but was not executed through a real team-manager and platform-reviewer browser session because no disposable authenticated identities were available in the final local session. It remains part of the M3 full-acceptance E2E scope.
