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

// R4 · record delivery
await enqueue('DELIVERY_RECORDED', {
  stopId,
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

## Swapping in the shared API client

`transport.ts` is the only file that calls `fetch`. When the shared `lib/api`
client lands (Shaanil's app shell), pass it in — nothing else changes:

```ts
import { setSyncTransport } from 'lib/offline'
setSyncTransport((items) => api.post('/api/sync', { items }))
```

---

## For the loader app (VihanJ)

Identical queue, one extra cache key per dock. Two things to know:

1. The loader is a **shared tablet**. `startSyncRunner()` is safe to call on
   every mount — it clears its own interval and listeners first — but call it once
   from the loader layout rather than per screen.
2. Photo and signature uploads are queued the same way, but they carry binary
   data. `enqueue()` takes JSON only, so uploads go through the outbox as a
   separate item type when that lands with the file endpoint
   (`POST /api/driver/files`, docs/api.md §9). Do not base64 a 4 MB photo into
   an outbox row — it will blow past the IndexedDB quota on a tablet that has been
   in a dock all day.

---

## Not built yet

| What | Why |
|---|---|
| Offline file upload | Needs `POST /api/driver/files` and the `files` table (V3, Manula) |
| Conflict list for R6 | Needs `GET /api/dispatch/conflicts`; `sync_log` records the result, the screen reads the conflict |
| Tests | The frontend test runner lands with the app shell; these functions are written to be injected (`setSyncTransport`) so they test without a browser |