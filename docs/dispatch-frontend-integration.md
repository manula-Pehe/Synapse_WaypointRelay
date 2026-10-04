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

At the initial integration commit, no missing backend endpoints were implemented. The live board, reports, and capacity screens still display sample data because their contracted backend endpoints are absent. The previous mock order, fleet, and outlets components remain in the repository, but the dispatcher route uses the connected pages instead.

This branch was verified with `npm run build`, `npm run lint`, `npm test`, and `git diff --check`. It has not been exercised against a running seeded backend or browser. No competition dataset was read or committed.

## Frontend follow-up after commit `da0fc8e`

The initial integration was committed and pushed as `da0fc8e`. The next commit adds:

- **F1:** added reusable UI components, status badges, icons, a `/ui` showcase, and a theme switch with a dark default for the driver placeholder. The repository contains temporary tokens but not the submitted Figma design system or its Sinhala/Tamil font files; exact Figma fidelity and bundled fonts remain pending that reference. The dark palette reuses values already present in the project's token file.
- **F2:** the dispatcher header now reads the latest plan status and real notification count. The notification drawer lists and marks backend notifications read. The Issues nav opens the existing connected issue screen. Screens whose backend is absent are labeled as sample previews.
- **F3:** added status, brand, temperature, and outlet filters, actual weight/volume columns, chilled tags, and a fuller order detail timeline.
- **F4:** grouped fleet rows by vehicle type and temperature and shows the available weekly fuel quota field. Fuel used percentage is not in the fleet response, so it is not shown.
- **F7:** the account menu now shows the signed-in dispatcher, saves language through `PATCH /api/auth/me`, and signs out through the shared auth context.
- **F9:** added a schematic district network map from the existing plan and order endpoints, showing trip counts and order-derived status without claiming GPS positions. A plan is required before it can show trips.

No backend code was changed for this follow-up. Run `npm run build`, `npm run lint`, and `npm test` in `frontend/` to verify it.
