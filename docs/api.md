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
| 409 | `INVALID_STATUS`, `ORDERS_CLOSED`, `ORDERS_NOT_CLOSED`, `PLAN_LOCKED`, `RULE_VIOLATION`, `DUPLICATE` |

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

---

## 3. Reference data

### `GET /api/outlets?depot=&brand=` → list of
```json
{ "id": "OUT001", "brand": "Fresh", "district": "Colombo", "depot": "Peliyagoda",
  "dockType": "street", "parkingConstraint": "van_only",
  "windowOpen": "05:00", "windowClose": "07:30", "mallWindowOpen": null, "mallWindowClose": null }
```
### `GET /api/outlets/{id}` → one outlet

### `GET /api/vehicles?depot=&runDate=` → list of
```json
{ "id": "VEH036", "type": "van", "temp": "reefer", "weightCapKg": 1040, "volumeCapM3": 7.0,
  "fuelType": "diesel", "kmPerL": 9.5, "weeklyFuelQuotaL": 300, "depot": "Peliyagoda",
  "availability": "AVAILABLE", "availabilityReason": null }
```

### Fleet (dispatcher) — D2, D2v
- `GET /api/dispatch/fleet?runDate=&depot=` → `{ items: [vehicle…], confirmedAt, confirmedBy, counts: { available, inWorkshop, offRoad, reeferAvailable } }`
- `PUT /api/dispatch/fleet/{vehicleId}` `{ "runDate": "2026-10-01", "status": "OFF_ROAD", "reason": "Brake issue" }` → vehicle
- `POST /api/dispatch/fleet/confirm` `{ "runDate", "depot" }` → `{ confirmedAt, confirmedBy }`

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
- `GET /api/orders/close-status?runDate=&depot=` (any role) → `{ closed: true, closedAt, closedBy }`

### Dispatcher
- `POST /api/dispatch/orders/close` `{ "runDate", "depot" }` → `{ closedAt, confirmed: 79, autoConfirmed: 3, notConfirmed: 3 }` — D1 button; 409 `ORDERS_CLOSED` if already closed
- `POST /api/dispatch/orders/phone-in` `{ "outletId", "runDate", "temp", "units", "note" }` → order (`source=PHONE_IN`, `storeChecked=false`) — D1b
- `GET /api/dispatch/orders/unconfirmed?runDate=&depot=` → `{ items: [ { outletId, outletName, phone, orders: [order…] } ] }` — D1u

---

## 5. Notifications

### Notification object
```json
{ "id": "ntf-1", "severity": "CRITICAL", "type": "DELIVERY_FAILED", "title": "Failed delivery · OUT012",
  "body": "Store closed. 64 cases returning.", "link": "/dispatch/live/failed/123",
  "createdAt": "…", "readAt": null }
```
- `GET /api/notifications?unread=true` → list (newest first) + `unreadCount`
- `POST /api/notifications/{id}/read` · `POST /api/notifications/read-all`

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
- `POST /api/store/orders` `{ "runDate", "temp", "units", "note" }` → order (`source=STORE`) — S2n
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
| `OrderService` | core | `get(id)`, `findByRun(runDate, depot, filters)`, `confirm(id)`, `editUnits(id, units)`, `cancel(id, reason)`, `createStoreOrder(…)`, `createPhoneInOrder(…)`, `markPlanned(id, planId)`, `markMoved(id, newDate, reason)`, `markLoaded(id)`, `markOnTheWay(id)`, `recordOutcome(id, outcome, units)`, `createRemainder(parentId, units, reason)`, `history(id)`, `isClosed(runDate, depot)` |
| `ReferenceService` | core | `outlet(id)`, `outlets(depot)`, `vehicle(id)`, `availableVehicles(runDate, depot)`, `travel(district, depot)`, `serviceMinutes(brand, dockType)`, `fuelUsed(vehicleId, isoYear, isoWeek)` |
| `NotificationService` | notification | `notifyUser(userId, …)`, `notifyRole(role, scope, severity, type, title, body, link)` |
| `FileService` | core | `store(bytes, contentType, kind, clientId): fileId`, `get(fileId)` — photos and signatures (driver proof, issue photos) |
| `PlanQueryService` | planning | `tripsForVehicle(runDate, vehicleId)`, `tripsForDepot(runDate, depot)`, `stopForOrder(orderId)`, `deferralForOrder(orderId)`, `publishedPlan(runDate, depot)` |
| `DeliveryQueryService` | driver | `deliveryForOrder(orderId)`, `driverStatus(vehicleId)`, `failedDeliveries(runDate)`, `openConflicts(runDate)`, `vehicleProblems(runDate)` |
| `LoadingQueryService` | loader | `loadingStatus(runDate, depot)`, `shortfallForOrder(orderId)` |

Until a service is merged, callers code against its interface and use a stub returning sample data.
