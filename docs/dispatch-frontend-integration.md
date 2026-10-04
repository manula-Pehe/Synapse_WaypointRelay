# Dispatcher frontend integration

Branch: `codex/connect-dispatch-backend`

## Changes

- Added a typed dispatcher API client using the existing authenticated `frontend/src/lib/api` helper.
- Connected the dispatcher order queue to orders, unconfirmed outlets, close status, order history, close orders, and phone-in order endpoints. Lists refresh after successful mutations.
- Connected fleet availability, per-date updates, and fleet confirmation to the backend. An unavailable vehicle requires a reason.
- Connected the read-only outlets view to the reference endpoint and added client-side search.
- Connected the dispatcher header to the demo settings endpoint and clock mutation. The clock input is Sri Lanka local time and sends `+05:30` as required by `docs/api.md`.
- Updated the shared API client's environment lookup so the Node test runner can import it; added dispatcher request tests.

## Limits

No missing backend endpoints were implemented. The live board, reports, and capacity screens still display sample data because their contracted backend endpoints are absent. The network map is not implemented. The previous mock order, fleet, and outlets components remain in the repository, but the dispatcher route uses the connected pages instead.

This branch was verified with `npm run build`, `npm run lint`, `npm test`, and `git diff --check`. It has not been exercised against a running seeded backend or browser. No competition dataset was read or committed.
