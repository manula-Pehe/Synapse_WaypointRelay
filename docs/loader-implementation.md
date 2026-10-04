# Loader implementation log

Branch: `codex/loader-vihanj`. Scope: F1–F6 of the VihanJ loader brief. The brief and exported PNGs are design references; behavior below reflects code in this branch.

## F1 · Shared dock sign-in

- Added `/loader/sign-in` with a four-digit PIN keypad and failure state. It calls the existing auth endpoint with `identifier=PELIYAGODA`, keeps the token in session storage, and clears a wrong PIN.
- Added a loader shell with the signed-in loader, switch action, dock context, demo-clock time, and connectivity indicator. The indicator says **Online** only when the settings request succeeds; it does not claim data has synced offline.
- Allowed unauthenticated `GET /api/settings` so the sign-in header can show the server demo clock. The clock-changing POST remains dispatcher-only.
- Unauthenticated loader routes redirect to the dock sign-in screen.

## F2 · Trips to load

- `GET /api/loader/trips` uses the published plan for the signed-in loader's depot, sorts by departure with chilled trips first on a tie, and returns the 3:30 AM list time. There are no fabricated trips before publication.
- Status comes from recorded ticks and handover. A handed-over trip becomes `DEPARTED` only after all its orders are `ON_THE_WAY` or later. Trip 2 remains unavailable until the same vehicle's previous trip has departed.
- The trip list shows vehicle, trip number, district, brand, stops, cases, chilled status, departure time, and loading status. An unpublished plan shows the waiting screen.

## F3 · Fridge check

- `POST /api/loader/trips/{id}/fridge-check` records the loader, demo-clock time, unit status, temperature, doors, and pass result. A pass needs the unit running, 0–5 °C inclusive, and doors OK.
- The latest failed check blocks chilled loading and handover, alerts dispatch, and can be repeated. `V51__fridge_check_order.sql` provides insertion order when several checks share the same demo-clock second.

## F4 · Loading list

- `GET /api/loader/trips/{id}` returns the published trip, vehicle capacities, reverse stop order (`load_seq`), recorded outlet access details and store order note, effective cases after shortfalls, proportional load weight/volume, tick state, and latest fridge check.
- `POST /api/loader/stops/{stopId}/tick` records the loader and demo-clock time. All stops ticked moves the trip to `READY`. The UI shows progress, capacity bars, and stop details.

## F5 · Shortfall

- `POST /api/loader/stops/{stopId}/shortfall` validates units and reason, creates a remainder through `OrderService.createRemainder`, records the shortfall, reduces cases and weight/volume shown as loaded, and notifies the store and dispatcher. Shortfall reporting is allowed before handover, including after a stop was ticked.
- A full-stop shortfall is rejected and directed to dispatch, per the user’s decision, because the current order lifecycle has no zero-case loaded state.
- `LoadingQueryService.shortfallForOrder(orderId)` exposes the recorded remainder for other modules. A stop can have one shortfall report; repeat reports return a duplicate error.

## F6 · Driver handover

- `POST /api/loader/trips/{id}/handover` requires all stops ticked, a passing fridge check for chilled loads, weight/volume within vehicle capacity, and an active driver whose staff ID belongs to that vehicle. It stores the handover and changes each planned order through `OrderService.markLoaded` in one transaction.
- `LoadingQueryService.loadingStatus(runDate,depot)` exposes trip loading facts. The UI shows the case and weight summary, driver confirmation, and next available trip. Handover reports `LOADED`, which is distinct from departure.

## Data and access

- `V50__loader.sql` adds `trip_loading`, `fridge_checks`, `load_ticks`, `shortfalls`, and `handovers`. No planning tables are changed.
- Loader API access remains role restricted. Trip and stop operations resolve the published plan and check the signed-in depot. Mutations lock the trip row so concurrent ticks, shortfalls, checks, and handover serialize.

## Verification

- Frontend production build, ESLint, and the existing frontend test suite pass. The sign-in view was inspected at a 390 px viewport.
- Backend compiles. PostgreSQL workflow tests cover depot scope, trip 2 availability, recorded store notes, failed and repeated fridge checks, partial and full shortfalls, missing reason validation, remainder/history, and handover order status. Security tests cover public demo-clock reading and protected clock changes.

## Known design/data gaps

- Store notes exist only for store-created orders that included a note in their order history. Seeded orders without a recorded note show access information only.
- The Figma shortfall dialog includes a photo action. The F1–F6 API contract has no loader photo upload endpoint, so this action is not presented as functional.
- Handover changes the trip to `LOADED`. Departure is shown only after a driver updates the orders to `ON_THE_WAY`; the loader does not claim a truck has left at handover time.
