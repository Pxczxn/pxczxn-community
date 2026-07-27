# M3-T005 External Team Submissions

## Delivered

- Immutable external submission records in `team_submission`.
- The author submits a personal article's current fixed version to an active team.
- Idempotency keys, one active submission per source article/team, optimistic locking, and resubmission lineage.
- Team reviewers can approve, request revision, or reject through the team permission matrix.
- Platform reviewers can approve, request revision, or reject through `community:team:review`.
- Platform approval creates a separate published article and version in the target TEAM blog. The source article remains unchanged and original author attribution is retained.
- Team audit events and after-commit notification events are emitted for submission and review transitions.
- User-facing submission page at `/submissions` and legacy `/submissions/ai-agent`; admin platform queue at `/community/team-submissions`.

## API

- `POST /api/v1/team-submissions`
- `GET /api/v1/team-submissions/me`
- `GET /api/v1/team-submissions/teams/{teamId}`
- `POST /api/v1/team-submissions/{submissionId}/team/{approve|revision|reject}`
- `GET /admin-api/community/team-submissions`
- `POST /admin-api/community/team-submissions/{submissionId}/{approve|revision|reject}`

## Database Evidence

`V026__m3_team_submission.sql` was applied to the isolated MySQL 8.0.46 database `pxczxn_m3_t003`.

- Verify output: table `1`, required columns `7`, required indexes `3`.
- `R026__rollback_m3_team_submission.sql` removed the table successfully.
- Migration and verify were then applied again with the same `1`, `7`, `3` result.

## Validation

- Full backend `mvnw.cmd test` passed.
- Focused `TeamSubmissionServiceImplTest` passed: fixed snapshot, author boundary, independent published team article.
- Web `typecheck`, `lint`, `test`, and `build` passed.
- Admin `typecheck`, `lint`, `test`, and `build` passed. Existing repository lint warnings remain outside this task.
- Browser validation confirmed the new submission page at desktop and a 390px mobile viewport.
