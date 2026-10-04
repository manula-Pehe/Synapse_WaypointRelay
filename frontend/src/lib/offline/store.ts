import type { OutboxStatus, SyncResult, SyncState, SyncSummary } from './types'

/**
 * Everything the sync pill needs, in one immutable snapshot so React can read it
 * with useSyncExternalStore.*/
export interface SyncSnapshot {
  state: SyncState
  waiting: number
  /** Items that reached the server as APPLIED, DUPLICATE or CONFLICT. */
  done: number
  /** Items the server rejected , not a retry. */
  failed: number
  lastSyncAt: string | null
  lastResult: SyncResult | null
  lastSummary: SyncSummary | null
  online: boolean
}

const initial: SyncSnapshot = {
  state: 'synced',
  waiting: 0,
  done: 0,
  failed: 0,
  lastSyncAt: null,
  lastResult: null,
  lastSummary: null,
  online: true,
}

let snapshot: SyncSnapshot = initial

const listeners = new Set<() => void>()

function emit() {
  for (const listener of listeners) listener()
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function getSnapshot(): SyncSnapshot {
  return snapshot
}

/** Replace the snapshot and notify React. No-ops when nothing actually changed. */
export function update(patch: Partial<SyncSnapshot>): void {
  let changed = false
  for (const key of Object.keys(patch) as (keyof SyncSnapshot)[]) {
    if (snapshot[key] !== patch[key]) {
      changed = true
      break
    }
  }
  if (!changed) return
  snapshot = { ...snapshot, ...patch }
  emit()
}

/** Recompute the counters from the outbox after a write. */
export function setCounts(waiting: number, done: number, failed: number): void {
  update({ waiting, done, failed })
}

export function setState(state: SyncState): void {
  update({ state })
}

export function recordSync(at: string, summary: SyncSummary, result: SyncResult | null): void {
  update({ lastSyncAt: at, lastSummary: summary, lastResult: result })
}

/** Called when the browser reports a connection change. */
export function setOnline(online: boolean): void {
  update({ online, state: online ? (snapshot.waiting > 0 ? 'syncing' : 'synced') : 'offline' })
}

/** Statuses that still need to reach the server, in phone order. */
export const UNSENT: OutboxStatus[] = ['pending', 'sent']

/**
 * Fired on `window` after the outbox changes, so the sync runner can wake up
 */
export const OUTBOX_CHANGED = 'waypoint:outbox-changed'

export function announceChange(): void {
  window.dispatchEvent(new Event(OUTBOX_CHANGED))
}