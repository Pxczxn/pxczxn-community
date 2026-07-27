# M3-T010 Full Acceptance

## Current Scope

The M3 implementation is integrated on `M3-dev`. This acceptance work fixes
runtime defects found when exercising the real community and admin APIs and
records the evidence that has been executed against a disposable MySQL runtime.

## Runtime Defects Fixed

- Team-application idempotency is evaluated before the pending-application
  constraint, so an exact replay returns the original application instead of a
  business error.
- `Long` values are serialized as strings at the HTTP boundary. This prevents
  JavaScript precision loss for snowflake identifiers in web and admin flows.
- Public team-directory and team-portal reads are explicitly anonymous. Team
  workspace and all mutation APIs remain authenticated.
- The web team directory now presents real API data with dedicated loading,
  empty, error, and team-card states. Its client contracts match string IDs.
- The web dev command uses `--strictPort`; a busy `8847` fails immediately and
  never silently starts on another port.

## Executed Evidence

- `mvnw.cmd test`: 42 backend tests passed.
- Focused `CommunityPublicRoutePolicyTest`: 2 tests passed.
- Web `typecheck`, `lint`, `test`, and production `build`: passed.
- Admin `typecheck`, `lint`, `test`, and production `build`: passed; only
  pre-existing lint warnings remain.
- Real API E2E: `scripts/e2e/m3-team-lifecycle.mjs` passed against the
  disposable `pxczxn_m3_t011_runtime` database.

The E2E creates actual community users, performs application and invitation
idempotency, admin approval, outsider authorization rejection, invitation
acceptance, ownership transfer, member leave, and former-owner authorization
revocation. It also creates a real personal article, verifies collaboration
acceptance and rejection, submits that article's fixed version to the team,
executes team and platform approval through publication, and creates, orders,
submits, and approves a team series.

It is intentionally run only against a disposable database because team audit
events are append-only. All M3-T010 acceptance scenarios are now covered.
