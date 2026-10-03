// Shapes for the offline outbox. The wire contract is docs/api.md §9 (Sync);
// everything else here is local to the phone.

/** Action types the server understands on POST /api/sync. */
export const SYNC_TYPES = [
  'TRIP_ACCEPTED',
  'ARRIVED',
  'DELIVERY_RECORDED',
  'DELIVERY_UNDONE',
  'STORE_WAIT',
  'VEHICLE_PROBLEM',
] as const

export type SyncType = (typeof SYNC_TYPES)[number]

/** What the server did with one item. docs/api.md §9. */
export const SYNC_RESULTS = ['APPLIED', 'DUPLICATE', 'CONFLICT'] as const

export type SyncResult = (typeof SYNC_RESULTS)[number]

/** Local lifecycle of a queued item. Never sent to the server. */
export type OutboxStatus = 'pending' | 'sent' | 'done' | 'failed'

/** What the sync pill shows on every driver screen. */
export type SyncState = 'synced' | 'syncing' | 'offline'

export type JsonValue = string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue }

export type JsonObject = { [key: string]: JsonValue }

/** One row of the outbox. `seq` is the phone's FIFO order; `clientId` is the idempotency key. */
export interface OutboxRow {
  seq?: number
  clientId: string
  type: SyncType
  payload: JsonObject
  createdAt: string
  status: OutboxStatus
  attempts: number
  result?: SyncResult
  lastError?: string
}

/** One item of the POST /api/sync request body. */
export interface SyncRequestItem {
  clientId: string
  type: SyncType
  createdAt: string
  payload: JsonObject
}

export interface SyncRequest {
  items: SyncRequestItem[]
}

export interface SyncResponseItem {
  clientId: string
  result: SyncResult
  entityId?: string
}

export interface SyncResponse {
  results: SyncResponseItem[]
}

export interface SyncSummary {
  sent: number
  applied: number
  duplicate: number
  conflict: number
  failed: number
}