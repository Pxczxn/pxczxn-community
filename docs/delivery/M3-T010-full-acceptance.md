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

The E2E creates actual community users, performs application idempotency,
admin approval, invitation idempotency, outsider authorization rejection,
invitation acceptance, and ownership transfer. It is intentionally run only
against a disposable database because team audit events are append-only.

## Remaining M3 Acceptance Work

- Cross-role real E2E for member leave after ownership transfer.
- Fixed-version external submission and team/platform review through publish.
- Series chapter ordering and platform review.
- Collaboration invitation accept and reject flows.

M3-T010 remains `开发中` until these cases are exercised against the real APIs.
