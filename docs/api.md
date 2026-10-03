# Waypoint Relay — API Contract

> The contract between the frontend and backend modules. Screens and endpoints are built to this document.
> Changes go through a pull request.

---

## 0. Conventions

| Rule | Value |
|---|---|
| Base path | `/api` (nginx / Vite proxy forwards to the backend) |
| Format | JSON, field names **camelCase** |
| Auth | `Authorization: Bearer <token>` on every request except `POST /api/auth/login` and `GET /actuator/health` |
| Dates | `YYYY-MM-DD` (e.g. `2026-10-01`) |
| Date-times | ISO-8601 with offset (e.g. `2026-10-01T05:30:00+05:30`); stored in UTC |
| Wall-clock times | `HH:mm` (e.g. `05:00`) for outlet windows |
| IDs | strings (`OUT001`, `VEH036`, uuid strings for orders) |
| Lists | `{ "items": [ … ], "total": 42 }` |
| Time source | always the **demo clock** (`GET /api/settings`), never the device clock |
| Depot values | `Peliyagoda`, `Kandy` (as in the dataset; matching is case-insensitive) |

### Error format (all endpoints)
```json
{ "code": "ORDERS_CLOSED", "message": "Orders closed — changes go to the next run", "details": {} }
```

| HTTP | Code(s) |
|---|---|
| 400 | `VALIDATION` (details = `{ field: message }`) |
| 401 | `UNAUTHORIZED` |
| 403 | `FORBIDDEN` |
| 404 | `NOT_FOUND` (also returned for other outlets'/depots'/vehicles' data) |
| 409 | `INVALID_STATUS`, `ORDERS_CLOSED`, `ORDERS_NOT_CLOSED`, `PLAN_LOCKED`, `RULE_VIOLATION`, `DUPLICATE`, `CONFLICT` (record changed by someone else — reload) |

### Enumerations
| Name | Values |
|---|---|
| Role | `STORE_MANAGER`, `DISPATCHER`, `LOADER`, `DRIVER` |
| OrderStatus | `PREPARED`, `CONFIRMED`, `PLANNED`, `LOADED`, `ON_THE_WAY`, `DELIVERED`, `PARTIAL`, `FAILED`, `MOVED`, `CANCELLED` |
| OrderSource | `SEED`, `HISTORY`, `STORE`, `PHONE_IN`, `REMAINDER` |
| Temp | `CHILLED`, `AMBIENT` |
| Brand | `Fresh`, `Style`, `Tech` |
| VehicleAvailability | `AVAILABLE`, `IN_WORKSHOP`, `OFF_ROAD` |
| PlanStatus | `DRAFT`, `PUBLISHED`, `SUPERSEDED` |
| DeferralKind | `UNAVOIDABLE`, `CHOSEN` |
| RuleCode | `BRAND_DISTRICT`, `FRIDGE_REQUIRED`, `VAN_ONLY`, `WRONG_DEPOT`, `OVER_WEIGHT`, `OVER_VOLUME`, `MAX_TRIPS`, `TIME_BUDGET`, `FUEL_QUOTA`, `WINDOW`, `NO_VEHICLE_FITS`, `FRIDGE_CAPACITY` |
| DeferralChoice | `KEEP`, `REDUCE`, `CANCEL`, `SPLIT` |
| TripLoadingStatus | `WAITING`, `LOADING`, `READY`, `LOADED`, `DEPARTED` |
| DeliveryOutcome | `DELIVERED`, `PARTIAL`, `FAILED` |
| Severity | `CRITICAL`, `WARNING`, `INFO` |
| SyncResult | `APPLIED`, `DUPLICATE`, `CONFLICT` |

### Order lifecycle (valid transitions)
```
PREPARED → CONFIRMED | CANCELLED
CONFIRMED → PLANNED | MOVED | CANCELLED
PLANNED → LOADED | MOVED
MOVED → CONFIRMED (next run) | CANCELLED
LOADED → ON_THE_WAY
ON_THE_WAY → DELIVERED | PARTIAL | FAILED
FAILED → MOVED | CANCELLED
```
Anything else → `409 INVALID_STATUS`.

---

## 1. Auth

### `POST /api/auth/login` (public)
```json
// request
{ "identifier": "dilani@waypoint.lk", "secret": "Relay@2026" }
// identifier: email (store, dispatcher) · staff ID "DRV-0036" (driver) · depot "Peliyagoda" (loader, secret = PIN)
// 200
{
  "token": "eyJ…",
  "user": { "id": "usr-dilani", "name": "Dilani J.", "role": "STORE_MANAGER",
            "outletId": "OUT001", "depot": null, "vehicleId": null, "language": "en", "theme": "system" }
}
// 401 UNAUTHORIZED — "Incorrect sign-in details."
```

### `GET /api/auth/me` → `user` object
### `PATCH /api/auth/me` `{ "language": "si", "theme": "dark" }` → `user`

---

## 2. Settings and demo clock

### `GET /api/settings` (any role)
```json
{ "runDate": "2026-10-01", "now": "2026-09-30T14:00:00+05:30", "timezone": "Asia/Colombo" }
```
### `POST /api/settings/clock` (dispatcher) `{ "at": "2026-09-30T16:05:00+05:30" }` → same as GET

Moving the clock forward runs every timed job that has become due (see `docs/deployment.md`, "Timed jobs") before the response returns, so e.g. a move past 4 PM has closed the orders by then. Moving it backwards re-runs nothing.

---

## 3. Reference data

Any signed-in role can read outlets and vehicles. `depot` matches case-insensitively; leaving it out returns every depot.

### `GET /api/outlets?depot=&brand=` → list of
```json
{ "id": "OUT001", "name": "OUT001 · Colombo", "brand": "Fresh", "district": "Colombo", "depot": "Peliyagoda",
  "dockType": "street", "parkingConstraint": "van_only",
  "windowOpen": "05:00", "windowClose": "07:30", "mallWindowOpen": null, "mallWindowClose": null }
```
Ordered by id. `name` is "id · district" (same as on orders) until outlets have a name column. `brand` filters ignoring case.
### `GET /api/outlets/{id}` → one outlet (404 `NOT_FOUND` if unknown)

### `GET /api/vehicles?depot=&runDate=` → list of
```json
{ "id": "VEH036", "type": "van", "temp": "reefer", "weightCapKg": 1040, "volumeCapM3": 7.0,
  "fuelType": "diesel", "kmPerL": 9.5, "weeklyFuelQuotaL": 300, "depot": "Peliyagoda",
  "availability": "AVAILABLE", "availabilityReason": null }
```
Ordered by id. `runDate` defaults to the current run date.

**Availability rule (one place, used by every endpoint and by `ReferenceService`):** a vehicle with **no** `vehicle_availability` row for the run date is `AVAILABLE`. A row can mark it `IN_WORKSHOP` or `OFF_ROAD` for that date only.

### Fleet (dispatcher) — D2, D2v
- `GET /api/dispatch/fleet?runDate=&depot=` → `{ items: [vehicle…], confirmedAt, confirmedBy, counts: { available, inWorkshop, offRoad, reeferAvailable } }`
  - `items` are the depot's vehicles with availability; `counts` are per status, and `reeferAvailable` counts only **available** reefers; `confirmedAt` / `confirmedBy` are `null` until the fleet is confirmed.
  - `runDate` defaults to the current run date. `depot` defaults to the dispatcher's own depot; a dispatcher with no depot must send it (400 `VALIDATION`). A depot name no outlet uses → 400 `VALIDATION`; another real depot than the dispatcher's own → 404.
- `PUT /api/dispatch/fleet/{vehicleId}` `{ "runDate": "2026-10-01", "status": "OFF_ROAD", "reason": "Brake issue" }` → vehicle (with its new availability) — D2v
  - `reason` is required (non-blank, max 200) unless `status` is `AVAILABLE`, otherwise 400 `VALIDATION`; for `AVAILABLE` the stored reason is cleared.
  - Unknown vehicle → 404. A dispatcher who has a depot cannot change another depot's vehicle (404); a dispatcher with no depot covers all depots.
  - `updatedBy` / `updatedAt` come from the signed-in user and the demo clock.
- `POST /api/dispatch/fleet/confirm` `{ "runDate", "depot" }` → `{ confirmedAt, confirmedBy }` — writes the run's fleet confirmation. Confirming again is fine and updates the time. A depot name no outlet uses → 400 `VALIDATION` (as for close orders); another real depot than the dispatcher's own → 404.

---

## 4. Orders

### Order object
```json
{
  "id": "4f1c…", "ref": "S1-001", "outletId": "OUT001", "outletName": "OUT001 · Colombo",
  "brand": "Fresh", "temp": "CHILLED", "units": 80, "weightKg": 448.6, "volumeM3": 2.445,
  "runDate": "2026-10-01", "status": "PREPARED", "source": "SEED", "autoConfirm": false,
  "daysSinceLastServed": 2, "deferredYesterday": false, "parentOrderId": null,
  "storeChecked": true, "confirmedAt": null, "updatedAt": "…"
}
```

- `GET /api/orders?runDate=&depot=&status=&outletId=&brand=&temp=` (dispatcher; store sees own outlet only) → list
- `GET /api/orders/{id}` → `{ order, history: [ { at, actor, type, fromStatus, toStatus, details } ] }` — D10, S3p
- `runDate` defaults to the current run date; `depot` matches case-insensitively; results are ordered by `ref`. A store manager always gets their own outlet, whatever `outletId` they pass.
- `history[].actor` is the user id (`null` when the system or a timed job made the change); `type` is the new status name, or `EDITED` for a quantity change.
- `GET /api/orders/close-status?runDate=&depot=` (any role) → `{ closed, closedAt, closedBy, cutOffAt }` — `runDate` defaults to the current run date, `depot` is required. `cutOffAt` is 4:00 PM Sri Lanka time on the day before `runDate`; `closedAt` and `closedBy` are `null` while open, and `closedBy` is also `null` when the 4 PM job closed the orders

- After the orders of a run are closed, a **store manager's** confirm, edit and cancel of its orders (and a new store order) return 409 `ORDERS_CLOSED`. The dispatcher and timed jobs are not restricted.

### Dispatcher
- `POST /api/dispatch/orders/close` `{ "runDate", "depot" }` → `{ closedAt, confirmed: 79, autoConfirmed: 3, notConfirmed: 3 }` — D1 button; 409 `ORDERS_CLOSED` if already closed; 400 `VALIDATION` for an unknown depot. Counts are for that run and depot: `confirmed` = orders already confirmed before closing, `autoConfirmed` = Fresh ambient orders confirmed by the cut-off (`autoConfirm=true`, history `details.auto`), `notConfirmed` = orders left `PREPARED` (chilled, Style, Tech) — planning uses only `CONFIRMED` orders. `depot` matches case-insensitively
- `POST /api/dispatch/orders/phone-in` `{ "outletId", "runDate", "temp", "units", "note" }` → `201` + order (`source=PHONE_IN`, `status=CONFIRMED`, `storeChecked=false`; weight and volume are estimated from the outlet's past orders) — D1b. 409 `ORDERS_CLOSED` when the orders of that `runDate` and the outlet's depot are already closed; a later run date is still open
- `GET /api/dispatch/orders/unconfirmed?runDate=&depot=` → `{ items: [ { outletId, outletName, phone, orders: [order…] } ], total }` — D1u. Lists `PREPARED` orders, ordered by outlet id; `phone` is `null` until outlets store one

---

## 5. Notifications

### Notification object
```json
{ "id": "ntf-1", "severity": "CRITICAL", "type": "DELIVERY_FAILED", "title": "Failed delivery · OUT012",
  "body": "Store closed. 64 cases returning.", "link": "/dispatch/live/failed/123",
  "createdAt": "…", "readAt": null }
```
- `GET /api/notifications?unread=true` → `{ "items": [notification…], "total": n, "unreadCount": n }`, newest first. Without `unread` all of the user's notifications are listed. `total` counts the returned items; `unreadCount` always counts every unread notification of the user.
- `POST /api/notifications/{id}/read` → the notification with `readAt` set. Reading it again keeps the first `readAt`.
- `POST /api/notifications/read-all` → `{ "updated": n }` (how many were newly marked read).
- Open to every signed-in role; a user only ever sees and changes their own notifications. Someone else's or an unknown id → 404 `NOT_FOUND`.
- A notification is stored once per recipient, so read state is per user. `createdAt` is the demo clock time.
- **Critical notifications can't be muted.** There is no muting yet, so nothing filters notifications out; when muting is added it must skip `CRITICAL`.

---

## 6. Planning

### Plan object (`GET /api/dispatch/plans/{id}`)
```json
{
  "id": "pln-1", "runDate": "2026-10-01", "depot": "Peliyagoda", "version": 1, "status": "DRAFT",
  "summary": { "served": 80, "deferred": 5, "unavoidable": 1, "chosen": 4, "violations": 0,
               "fridgeVehiclesUsed": 4, "fridgeVehiclesAvailable": 4 },
  "vehicles": [
    { "vehicleId": "VEH036", "type": "van", "temp": "reefer", "weightCapKg": 1040, "volumeCapM3": 7.0,
      "freshMinutesUsed": 127, "freshBudget": 270, "daytimeMinutesUsed": 0, "daytimeBudget": 480,
      "trips": [
        { "id": "trp-1", "tripNo": 1, "brand": "Fresh", "district": "Colombo", "windowType": "FRESH",
          "departAt": "…", "minutes": 64, "weightKg": 1032, "volumeM3": 5.7,
          "stops": [ { "id": "stp-1", "orderId": "…", "orderRef": "S1-005", "outletId": "OUT003",
                       "seq": 1, "loadSeq": 2, "units": 42, "temp": "CHILLED",
                       "arriveFrom": "…", "arriveTo": "…", "lateRisk": 0.1 } ] } ] } ]
}
```
### Deferral object
```json
{ "id": "dfr-1", "orderId": "…", "orderRef": "S1-058", "outletId": "OUT054", "kind": "CHOSEN",
  "rule": "FRIDGE_CAPACITY", "reason": "All 4 fridge vehicles are full before 8 AM.",
  "priorityScore": 3, "daysWaited": 2, "newDate": "2026-10-02", "needsDecision": false,
  "storeChoice": null }
```

### Endpoints
- `GET /api/dispatch/plans/readiness?runDate=&depot=` → `{ ordersClosed, fleetConfirmed, confirmedOrders, availableVehicles, reeferAvailable, warnings: [] }` — Dp0
- `POST /api/dispatch/plans` `{ "runDate", "depot" }` → plan (DRAFT); 409 `ORDERS_NOT_CLOSED` — Dp1/Dp2
- `GET /api/dispatch/plans?runDate=&depot=` → latest plan (or 404)
- `GET /api/dispatch/plans/{id}` → plan · `GET /api/dispatch/plans/{id}/deferrals` → list — D3, D4
- `POST /api/dispatch/plans/{id}/validate-move` `{ "orderId", "toTripId" | "toVehicleId" }` → `{ ok, rule, message }` — D5
- `POST /api/dispatch/plans/{id}/move` (same body) → plan; 409 `RULE_VIOLATION`
- `POST /api/dispatch/plans/{id}/publish` → plan (PUBLISHED); 409 `PLAN_LOCKED` — D3p/D3ok
- `POST /api/dispatch/plans/{id}/revise` `{ "reason", "changes": [ { "orderId", "toTripId" } ] }` → plan v2 — D3r
- `POST /api/dispatch/plans/{id}/breakdown` `{ "vehicleId", "problemId" }` → `{ suggestions: [ { vehicleId, tripNo, stops, newArrive } ] }`; then `revise` — D6b

### Store-facing
- `POST /api/store/deferrals/{id}/choice` `{ "choice": "REDUCE", "units": 120 }` → deferral (S4k, S4r, S4x, S4u, S2c)

---

## 7. Store

- `GET /api/store/home` → `{ outlet, runDate, ordersClosed, cutOffAt, tomorrow: [order…], today: [delivery…], openIssues }` — S1
- `GET /api/store/orders?from=&to=` → list of orders (own outlet) — S6
- `PUT /api/store/orders/{id}` `{ "units": 80 }` → order (PREPARED only; 409 `ORDERS_CLOSED` / `INVALID_STATUS`) — S2
- `POST /api/store/orders/{id}/confirm` → order · `POST /api/store/orders/{id}/cancel` → order — S2, S2x
- `POST /api/store/orders` `{ "runDate", "temp", "units", "note" }` → order (`source=STORE`) — S2n; 409 `ORDERS_CLOSED` when that run is closed
- `POST /api/store/orders/{id}/check` `{ "ok": true }` or `{ "ok": false, "message": "…" }` — S2e, S2e-msg
- `GET /api/store/deliveries?runDate=` → list of
```json
{ "orderId": "…", "orderRef": "S1-001", "status": "PLANNED",
  "arrival": { "from": "…", "to": "…", "lateRisk": 0.38, "vehicleId": "VEH036", "tripNo": 2, "changedReason": null },
  "deferral": null,
  "delivery": null,          // after delivery: { outcome, units, photoUrl, signatureUrl, receivedBy, at }
  "shortfall": null,         // { missingUnits, reason, remainderOrderRef }
  "driverStatus": null,      // { offline: true, lastSyncAt }
  "receipt": null }          // { receivedUnits, at }
```
- `POST /api/store/orders/{id}/receipt` `{ "receivedUnits": 78, "note": "" }` — S5
- `POST /api/store/failed/{deliveryId}/choice` `{ "choice": "REPLAN_TOMORROW" | "TRY_LATER_TODAY" | "CANCEL" }` — S3f
- `POST /api/store/breakdown/{stopId}/choice` `{ "accept": true }` — S3k

### Issues (store + dispatcher)
- `POST /api/store/issues` `{ "orderId", "type": "DAMAGED", "units": 2, "note", "wants": "REPLACE" }` (+ photos via multipart) — S8
- `GET /api/store/issues` · `GET /api/store/issues/{id}` · `POST /api/store/issues/{id}/messages` `{ "text" }` — S9, S9d
- `GET /api/dispatch/issues?status=` · `POST /api/dispatch/issues/{id}/reply` `{ "text" }` · `POST /api/dispatch/issues/{id}/resolve` — D11, D11d

---

## 8. Loader

- `GET /api/loader/trips?runDate=` → list of `{ tripId, vehicleId, tripNo, district, brand, stops, units, chilled, departAt, status }` (empty + `listsAvailableAt` before publish) — L1b, L1w
- `GET /api/loader/trips/{id}` → `{ trip, vehicle, stops: [ { stopId, loadSeq, outletId, units, weightKg, volumeM3, accessNote, ticked } ], fridgeCheck }` — L2, L2d
- `POST /api/loader/trips/{id}/fridge-check` `{ "running": true, "tempC": 3, "doorsOk": true }` → `{ passed }` — L2f
- `POST /api/loader/stops/{stopId}/tick` — L2
- `POST /api/loader/stops/{stopId}/shortfall` `{ "missingUnits": 2, "reason": "MISSING", "note": "" }` → `{ remainderOrderRef }` — L3
- `POST /api/loader/trips/{id}/handover` `{ "driverStaffId": "DRV-0036" }` → `{ status: "LOADED", at }` — L5

---

## 9. Driver

- `GET /api/driver/today` → `{ vehicle, runDate, trips: [ { tripId, tripNo, district, departAt, loadingStatus, stops: [ { stopId, orderId, orderRef, seq, outlet, units, temp, arriveFrom, arriveTo, status } ] } ] }` — R1, R2
- `POST /api/driver/trips/{id}/accept` — R0
- `POST /api/driver/files` (multipart: `file`, `kind=PHOTO|SIGNATURE`, `clientId`) → `{ fileId, url }`
- `POST /api/driver/problems` `{ "tripId", "kind", "canDrive", "fridgeTempC", "note" }` — R9
- `GET /api/driver/summary` — R10

### Sync (driver; loader if time)
`POST /api/sync`
```json
// request — ordered
{ "items": [
  { "clientId": "c0a8…", "type": "DELIVERY_RECORDED", "createdAt": "…",
    "payload": { "stopId": "stp-1", "outcome": "DELIVERED", "units": 42, "receivedBy": "Sunil",
                 "photoFileId": "f-1", "signatureFileId": "f-2", "arrivedAt": "…", "completedAt": "…" } }
] }
// 200
{ "results": [ { "clientId": "c0a8…", "result": "APPLIED" } ] }
```
Item types: `TRIP_ACCEPTED`, `ARRIVED`, `DELIVERY_RECORDED`, `DELIVERY_UNDONE`, `STORE_WAIT`, `VEHICLE_PROBLEM` (+ loader: `LOAD_TICK`, `SHORTFALL`, `HANDOVER` if time).

### Dispatcher cards from driver data
- `GET /api/dispatch/conflicts?runDate=` · `POST /api/dispatch/conflicts/{id}/resolve` `{ "resolution": "KEEP_FIELD" | "OVERRIDE" }` — D8, D8m
- `GET /api/dispatch/failed?runDate=` · `POST /api/dispatch/failed/{id}/decide` `{ "decision": "REPLAN_TOMORROW" | "TRY_LATER_TODAY" | "CANCEL" }` — D6f, D6fm
- `POST /api/dispatch/problems/{id}/reply` `{ "text" }` — R9ok

---

## 10. Dispatch operations

- `GET /api/dispatch/live?runDate=&depot=` → `{ kpis: { onTimePct, completedStops, deferredToday, skippedTwoPlus, fridgeUsePct }, attention: [ { kind: "CONFLICT" | "FAILED" | "VEHICLE_PROBLEM" | "LATE_RISK" | "SHORTFALL", severity, title, body, link } ], trips: [ { vehicleId, tripNo, district, stopsDone, stopsTotal, status, lastSyncAt } ] }` — D6, D6m
- `GET /api/dispatch/reports/run?runDate=&depot=` → run summary — D12
- `GET /api/dispatch/capacity?depot=&weeks=` → weekly estimate (rule-based) — D7 (if time)

---

## 11. Java service contracts (in-process, no HTTP between modules)

| Service | Module | Methods |
|---|---|---|
| `CurrentUser` | auth | `id()`, `role()`, `outletId()`, `depot()`, `vehicleId()` |
| `DemoClock` | core | `now(): OffsetDateTime`, `today(): LocalDate`, `runDate(): LocalDate` |
| `OrderService` | core | `get(id)`, `findByRun(runDate, depot, filters)`, `confirm(id)`, `editUnits(id, units)`, `cancel(id, reason)`, `createStoreOrder(…)`, `createPhoneInOrder(…)`, `markPlanned(id, planId)`, `markMoved(id, newDate, reason)`, `markLoaded(id)`, `markOnTheWay(id)`, `recordOutcome(id, outcome, units)`, `createRemainder(parentId, units, reason)`, `history(id)`, `isClosed(runDate, depot)`, `autoConfirm(id)` (cut-off only; no user) |
| `ReferenceService` | core | `outlet(id)`, `outlets(depot)`, `vehicle(id)`, `availableVehicles(runDate, depot)`, `travel(district, depot)`, `serviceMinutes(brand, dockType)`, `fuelUsed(vehicleId, isoYear, isoWeek)` — read-only, returns DTO records. `outlet`, `vehicle`, `travel` and `serviceMinutes` throw `NotFoundException` when nothing matches; `fuelUsed` returns 0 when no row exists; `vehicle(id)` shows availability for the current run date; `availableVehicles` applies the availability rule in §3 (also `outlets(depot, brand)` and `vehicles(runDate, depot)`) |
| `NotificationService` | notification | `notifyUser(userId, severity, type, title, body, link)`, `notifyRole(role, scope, severity, type, title, body, link)` — see below |
| `FileService` | core | `store(bytes, contentType, kind, clientId): fileId`, `get(fileId)` — photos and signatures (driver proof, issue photos) |
| `PlanQueryService` | planning | `tripsForVehicle(runDate, vehicleId)`, `tripsForDepot(runDate, depot)`, `stopForOrder(orderId)`, `deferralForOrder(orderId)`, `publishedPlan(runDate, depot)` |
| `DeliveryQueryService` | driver | `deliveryForOrder(orderId)`, `driverStatus(vehicleId)`, `failedDeliveries(runDate)`, `openConflicts(runDate)`, `vehicleProblems(runDate)` |
| `LoadingQueryService` | loader | `loadingStatus(runDate, depot)`, `shortfallForOrder(orderId)` |

**`NotificationService` details** (package `com.synapse.waypoint.notification.service`; `NotificationScope` and `NotificationSeverity` are in the notification module)
- `severity` is `CRITICAL`, `WARNING` or `INFO`; `type`, `title` and `body` are required (otherwise `VALIDATION`); `link` is optional.
- `notifyUser` throws `NotFoundException` when the user does not exist or is inactive.
- `notifyRole` writes one row per **active** user of the role inside the scope. Scope fields are `NotificationScope.outlet(id)`, `.depot(name)`, `.vehicle(id)` or `.none()`:

  | Role | Narrowed by |
  |---|---|
  | `STORE_MANAGER` | `outletId` |
  | `DRIVER` | `vehicleId` |
  | `LOADER` | `depot` |
  | `DISPATCHER` | `depot`; a dispatcher with no depot works across all depots and receives every depot's notifications |

  A missing scope field means the whole role (fine for broadcasts such as "all dispatchers"). **Store and driver notifications should always pass `outletId` / `vehicleId`**, otherwise every store manager or driver is notified.
- Writes join the caller's transaction (`REQUIRED`): if the caller's change rolls back, its notifications are not stored. Call the service from inside the same transaction as the change it reports.

Until a service is merged, callers code against its interface and use a stub returning sample data.
