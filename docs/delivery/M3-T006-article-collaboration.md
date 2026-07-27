# M3-T006 Article Collaboration

## Delivered

- `V027` adds article-scoped collaboration invitations, accepted collaborators, and append-only audit records.
- Invitations are idempotent, expire after 14 days, and support accept, reject, cancel, and author-initiated revocation.
- Accepted contributors have ordered attribution and a contribution type.
- Collaboration does not copy article content, versions, comments, or interactions.
- Default collaborators only receive attribution. Explicit `can_edit` grants editor view and edit access only for the current article.
- Collaboration never grants publish, delete, review, earnings, team management, or broader blog permissions.
- User APIs and `/collaboration/articles/{articleId}` provide a real collaboration surface backed by the service.

## Validation

- Isolated MySQL 8.0.46: V027 rollback, migration, and verify passed with 3 tables and 7 required indexes.
- `ArticlePermissionServiceTest` verifies explicit collaboration edit access cannot publish or delete.
- Full backend Maven test suite passed.
- Web typecheck, lint, test, and production build passed.
