# lib/offline - driver outbox and sync

Every action on the driver's phone is written here first and sent to the server
afterwards. The driver keeps working with no signal, and the work arrives exactly
once when the phone reconnects.

Owner: **Vihan**. Reused by the loader app: **VihanJ**.

Contract: `docs/api.md` §9 (`POST /api/sync`) and §11.

---

## How it works

```
driver taps "Delivered"
        │
        ▼
enqueue('DELIVERY_RECORDED', payload)   ← Dexie, IndexedDB. Screen already shows the result.
        │  clientId = crypto.randomUUID()
        ▼
syncRunner posts the pending batch, oldest first, to POST /api/sync
        │
        ├─ APPLIED     → done
        ├─ DUPLICATE   → done (we already had it — a retry is not a second delivery)
        └─ CONFLICT    → done, and the dispatcher gets a decision card (D8)
```

- **`clientId` is the idempotency key.** The server stores it uniquely in
  `sync_log`, so a batch that arrives twice is applied once. Never reuse one.
- **Order is the order the driver acted.** The outbox's primary key is an
  auto-increment `seq`; `clientId` is a separate unique index.
- **APPLIED, DUPLICATE and CONFLICT all mean "the server has it now".** Only a
  non-retryable 4xx leaves an item in `failed`, which the driver can see and a
  human can fix.
- **Retry uses exponential back-off with jitter** — 2 s, 4 s, 8 s … capped at 60 s.
  Jitter stops every phone in a dead spot reconnecting at the same instant.
- **The runner wakes on three signals:** an action was queued, the browser fires
  `online`, or 30 s pass as a safety net. Concurrent calls share one run.

---

## Using it

### 1. Start it once, near the app root

```ts
import { startSyncRunner } from 'lib/offline'

startSyncRunner()
```

### 2. Put the sync pill on every screen

Design system §6 — "sync pill everywhere".

```tsx
import { useSyncPillLabel, useSyncStatus } from 'lib/offline'

function SyncPill() {
  const { key, values } = useSyncPillLabel()
  const { state } = useSyncStatus()
  // key → 'driver.sync.synced' | 'driver.sync.syncing' | 'driver.sync.offline' | 'driver.sync.waiting'
  return <Pill tone={state}>{t(key, values)}</Pill>
}
```

Keys, not words — all driver text goes through i18n. `Offline` renders with the
cloud-off icon and the count: `Offline · 3 waiting`.

### 3. Never call a write endpoint from a screen

```ts
import { enqueue } from 'lib/offline'

// R4 · record delivery. The file ids come from uploading the proof first —
// see "For the loader app" below for why proof is not queued itself.
await enqueue('DELIVERY_RECORDED', {
  stopId,
  orderId,
  outcome: 'DELIVERED',
  units: 42,
  receivedBy: 'Kasun',
  photoFileId,
  signatureFileId,
  arrivedAt,
  completedAt,
})
```

The UI updates immediately. If the request never leaves the phone, the driver
still sees the delivery recorded — which is the whole point.

Action types: `TRIP_ACCEPTED`, `ARRIVED`, `DELIVERY_RECORDED`,
`DELIVERY_UNDONE`, `STORE_WAIT`, `VEHICLE_PROBLEM`. Loader adds `LOAD_TICK`,
`SHORTFALL`, `HANDOVER`.

### 4. Read cached data instead of fetching

Stop lists, outlet details and translations are downloaded at sign-in and read
from IndexedDB afterwards, so they survive a reload with no signal.

```ts
import { offlineDb } from 'lib/offline'

const today = await offlineDb.cache.get('today')
```

`cache` holds `today`, `outlets` and `i18n`, each with a `fetchedAt`. Never draw a
guessed position — show the last confirmed update and its window (design system §6).

---

## Transport

`transport.ts` posts the batch through the shared `lib/api` client, so a sync
carries the driver's bearer token like every other call. That matters offline: a
session signed in against the local PIN hash has no token, and the work must stay
queued until a real sign-in puts one back, rather than being sent and rejected.

To point it elsewhere — a different base URL, or a mock in tests:

```ts
import { setSyncTransport } from 'lib/offline'

setSyncTransport(async (items) => ({
  results: items.map((item) => ({ clientId: item.clientId, result: 'APPLIED' })),
}))
```

A transport throws `SyncTransportError(message, retryable)`. `retryable: false`
stops the loop and shows the driver what went wrong. A 4xx is never retried; a
5xx, 408 or 429 is.

---

## Offline sign-in (X1m-off)

`pin.ts` keeps a salted PBKDF2-SHA256 hash of the driver's PIN after a
successful **online** sign-in, so they can start a run at a depot with no signal.

```ts
import { checkPinOffline, rememberPin, forgetPin } from 'lib/offline'

const check = await checkPinOffline(staffId, pin)
// { ok, reason: 'ok' | 'no-stored-pin' | 'wrong-pin', credential }
```

`rememberPin` is called by `AuthProvider` only after the server has accepted the
credentials. `forgetPin` runs on sign-out. The staff id has to match the stored
one, so one driver's PIN cannot sign in as another on a shared phone.

**Be clear-eyed about what this is.** The hash is client-side, and a 4-digit PIN
is not a secret however it is stored. The offline session it mints carries an
`offline-` token and **no authority** — no endpoint accepts it. Actions queue in
the outbox, and the server still validates every one of them when they arrive.
It is a convenience for starting a run without signal, not a way around
authentication.

Covered by `frontend/tests/driver-offline.test.mjs`.

---

## For the loader app (VihanJ)

Identical queue, one extra cache key per dock. Three things to know:

1. The loader is a **shared tablet**. `startSyncRunner()` is safe to call on
   every mount — it clears its own interval and listeners first — but call it once
   from the loader layout rather than per screen.
2. **Photo and signature uploads do not go through the outbox.** `enqueue()` takes
   JSON only, and the driver app uploads the file first through
   `POST /api/driver/files` (docs/api.md §9), then enqueues the delivery naming the
   returned `photoFileId`. Uploads are idempotent on `clientId`, so a retry after a
   dropped connection does not create a second file.

   This means a delivery recorded with no signal waits for signal *before* its
   photo can be uploaded. Do not base64 a 4 MB photo into an outbox row as a
   workaround — it will blow past the IndexedDB quota on a tablet that has been in
   a dock all day. If the loader needs uploads to queue, that is a new item type
   and a design decision, not a small change.
3. A shared tablet has more than one driver's PIN. Give the loader its own storage
   key per staff id, or `rememberPin` will be overwritten by whoever signs in last.

---

## Still open

| What | Why |
|---|---|
| Uploads that queue offline | Proof needs signal before it uploads, and the handback signature is proof (see above) |

## Already landed since this doc was written

- `POST /api/driver/files` — proof upload, idempotent on `clientId`.
- `GET /api/dispatch/conflicts` + resolve — the D8 decision card reads it directly.
- The transport now uses `lib/api` rather than bare `fetch`.
- Offline sign-in (`pin.ts`) and its tests.
- **R8r — returned goods.** `GOODS_RETURNED` is a sync action like any other, so the handback is
  queued on the phone; the screen is `/driver/hand-back`, reached from the trip-end screen. It writes
  a `returns` row and does not touch the order — stock coming back on the shelf is not a delivery, so
  the shortfall stays outstanding. The signature (US-11.2) is uploaded first, because the row names
  the file; it is the one part of the handback that needs a signal, and a handback with no signature
  is still recorded rather than lost.
- **F12 — Sinhala / Tamil stop list.** `outletLabel()` (`features/driver/outlet.ts`) is the one place a
  store is rendered, and it reads the `outletName` the server already sends per stop. Everything else
  on the stop list goes through `t()`, including the two sentences that used to be hard-coded English.
  A name is a name in all three languages, which is why F12 is the label rather than the whole screen.
