import { offlineDb } from './db'
import { announceChange, setCounts, UNSENT } from './store'
import type {
  JsonObject,
  OutboxRow,
  SyncRequestItem,
  SyncResponseItem,
  SyncResult,
  SyncType,
} from './types'

/**
 * The outbox is the phone's queue of work waiting to reach the server. Driver
 * screens never call a write endpoint directly — they call enqueue() and the
 * screen updates immediately.
 */
export async function enqueue(type: SyncType, payload: JsonObject): Promise<string> {
  const row: OutboxRow = {
    clientId: crypto.randomUUID(),
    type,
    payload,
    createdAt: new Date().toISOString(),
    status: 'pending',
    attempts: 0,
  }
  await offlineDb.outbox.add(row)
  await refreshCounts()
  announceChange()
  return row.clientId
}

/** Items still waiting for the server, oldest first. */
export async function unsentItems(limit = 50): Promise<OutboxRow[]> {
  const rows = await offlineDb.outbox.where('status').anyOf(UNSENT).sortBy('seq')
  return rows.slice(0, limit)
}

/** The shape POST /api/sync expects — clientId, type, createdAt, payload only. */
export function toRequestItem(row: OutboxRow): SyncRequestItem {
  return { clientId: row.clientId, type: row.type, createdAt: row.createdAt, payload: row.payload }
}

export async function markSending(clientIds: string[]): Promise<void> {
  await offlineDb.outbox.where('clientId').anyOf(clientIds).modify({ status: 'sent' })
}

/**
 * APPLIED and DUPLICATE both mean the server now holds this action, so both are
 * done. CONFLICT is also done — the driver wrote a record that overrode a
 * dispatcher's change, and the dispatcher gets a decision card; retrying would
 * not change that.
 */
export async function markResults(results: SyncResponseItem[]): Promise<void> {
  for (const { clientId, result } of results) {
    await offlineDb.outbox.where('clientId').equals(clientId).modify({ status: 'done', result })
  }
  await refreshCounts()
}

/** The server said no in a way a retry cannot fix. Stop looping. */
export async function markFailed(clientId: string, error: string): Promise<void> {
  await offlineDb.outbox.where('clientId').equals(clientId).modify({ status: 'failed', lastError: error })
  await refreshCounts()
}

export async function countWaiting(): Promise<number> {
  return offlineDb.outbox.where('status').anyOf(UNSENT).count()
}

export async function refreshCounts(): Promise<void> {
  const [waiting, done, failed] = await Promise.all([
    countWaiting(),
    offlineDb.outbox.where('status').equals('done').count(),
    offlineDb.outbox.where('status').equals('failed').count(),
  ])
  setCounts(waiting, done, failed)
}

/** Drop synced rows so the outbox does not grow for the whole run. */
export async function pruneDone(): Promise<number> {
  return offlineDb.outbox.where('status').equals('done').delete()
}

/** Everything the R6 sync summary needs, oldest first. */
export async function history(): Promise<OutboxRow[]> {
  return offlineDb.outbox.orderBy('seq').toArray()
}

export function hasConflict(rows: OutboxRow[]): boolean {
  return rows.some((row) => row.result === ('CONFLICT' as SyncResult))
}