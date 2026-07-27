# M3-T008 Homepage, Discovery, and Copy Closure

## Delivered

- The root route now renders the community discovery experience rather than redirecting to `/discover`.
- The root page has its own Chinese homepage metadata and remains backed by the real article, moment, tag, and follow APIs used by the discovery feed.
- The primary `首页` navigation item now links to `/`; `发现` remains the explicit `/discover` route.
- The application mapper scan includes the team, submission, collaboration, and community-chat mapper packages. This restores packaged application startup after the M3 modules were added.
- User-facing planned capabilities remain honest states. Series and editorial selection are not represented as fabricated content.

## Administrative Navigation Review

- The admin primary menu is generated from `sys_menu` rather than the static compatibility routes.
- `V020__m25_community_information_architecture.sql` places platform personnel and permissions under system settings and hides the legacy organisation menu group from the primary menu.
- Community administration uses the approved community terminology for operations, review, governance, teams, taxonomy, notifications, chat, analytics, and system settings.

## Validation

- Backend Maven test suite: PASS.
- Backend production package: PASS.
- Web typecheck, lint, production build, and 5 rendered HTML tests: PASS.
- Admin typecheck, lint, tests, and production build: PASS. Existing lint warnings remain in legacy administration files and are unrelated to this task.
- Isolated MySQL runtime: `GET /api/v1/health` returned `UP`; public article and tag APIs returned `200`.
- Browser validation at `http://localhost:8848/`: homepage title rendered, the real empty feed state rendered, and the `首页` link resolved to `/`.
- Browser-origin CORS preflight from `http://localhost:8848` to the public article API returned the expected allowed origin and methods.

## Scope Note

The canonical M2.5-M6 breakdown still records series and serialization as an unimplemented M3 capability. The current M3 execution plan renumbered later work for collaboration, chat, and homepage closure. The next implementation task must reconcile that record and deliver the missing series capability rather than treating M3 as complete.
