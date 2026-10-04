# Dispatcher planning screens

This branch adds the dispatcher Plan tab from the existing planning API and the supplied Dp0, D3, D4, D3p and D3ok designs. No backend code or sample plan values were added.

## Flow and data

1. For the selected run date and depot, `GET /api/dispatch/plans` decides whether a plan exists. A 404 opens readiness; other errors are shown as errors.
2. Readiness uses `GET /api/dispatch/plans/readiness`. It shows recorded order, fleet, vehicle and reefer counts and backend warnings. Orders and fleet actions lead to their existing dispatcher screens. Create is enabled only when orders are closed, fleet is confirmed, and both confirmed orders and available vehicles are nonzero.
3. Create calls `POST /api/dispatch/plans`. The draft board uses the returned plan summary and vehicle/trip/stop data. Trip cards expand to show stop sequence, order, cases, arrival window and reverse loading sequence. Re-plan asks before replacing the current draft through the same endpoint.
4. Deferrals calls `GET /api/dispatch/plans/{id}/deferrals`. Its details show the backend's kind, rule, reason, priority, days waited, new date, decision flag and recorded store choice. The screen does not invent a dispatcher acceptance action.
5. Publish asks for confirmation, showing the actual served/deferred/vehicle/trip counts and notification categories described by the backend contract. It calls `POST /api/dispatch/plans/{id}/publish`, refreshes plan, deferral, live board and notification queries, then shows the published board as read-only.

The existing network map is linked from a published plan. The design's version 2 edit control is disabled because the current planning controller has no revise endpoint. Recent-plan history, a draft map and a deferral impact/accept action are also absent from the current API; the screen does not imply they are available.

## Verification

- `npm run build`
- `npm run lint`
- `npm test`

The changes are limited to the frontend Plan page, its API types/calls, and dispatcher routing, plus this record.
