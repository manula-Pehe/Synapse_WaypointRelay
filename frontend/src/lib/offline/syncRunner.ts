import {
  markFailed,
  markResults,
  markSending,
  refreshCounts,
  toRequestItem,
  unsentItems,
} from './outbox'
import { offlineDb } from './db'
import { OUTBOX_CHANGED, recordSync, setOnline, setState, subscribe } from './store'
import { defaultSyncTransport, SyncTransportError, type SyncTransport } from './transport'
import type { SyncRequestItem, SyncResponse, SyncResult, SyncSummary } from './types'

export { OUTBOX_CHANGED }

const BASE_BACKOFF_MS = 2_000
const MAX_BACKOFF_MS = 60_000
/** Safety net in case an enqueue event is missed. */
const POLL_MS = 30_000

const emptySummary: SyncSummary = { sent: 0, applied: 0, duplicate: 0, conflict: 0, failed: 0 }

let transport: SyncTransport = defaultSyncTransport
let failures = 0
let timer: number | undefined
let poller: number | undefined
let inFlight: Promise<SyncSummary> | undefined

export function setSyncTransport(next: SyncTransport): void {
  transport = next
}

/** Exponential back-off with jitter, so a whole depot of phones does not stampede. */
function backoffMs(): number {
  const raw = Math.min(BASE_BACKOFF_MS * 2 ** failures, MAX_BACKOFF_MS)
  return Math.round(raw * (0.75 + Math.random() * 0.5))
}

function scheduleRetry(): void {
  window.clearTimeout(timer)
  timer = window.setTimeout(() => {
    void syncNow()
  }, backoffMs())
}

function tally(results: SyncResponse['results']): SyncSummary {
  const summary: SyncSummary = { ...emptySummary, sent: results.length }
  for (const { result } of results) {
    if (result === ('APPLIED' as SyncResult)) summary.applied += 1
    else if (result === ('DUPLICATE' as SyncResult)) summary.duplicate += 1
    else if (result === ('CONFLICT' as SyncResult)) summary.conflict += 1
  }
  return summary
}

export function syncNow(): Promise<SyncSummary> {
  if (inFlight) return inFlight
  inFlight = run().finally(() => {
    inFlight = undefined
  })
  return inFlight
}

async function run(): Promise<SyncSummary> {
  if (!navigator.onLine) {
    setState('offline')
    return emptySummary
  }

  const rows = await unsentItems()
  if (rows.length === 0) {
    failures = 0
    setState('synced')
    return emptySummary
  }

  setState('syncing')
  const items: SyncRequestItem[] = rows.map(toRequestItem)
  await markSending(items.map((item) => item.clientId))

  try {
    const response = await transport(items)
    await markResults(response.results)
    failures = 0
    const summary = tally(response.results)
    const result: SyncResult | null = summary.conflict > 0 ? 'CONFLICT' : null
    recordSync(new Date().toISOString(), summary, result)
    setState('synced')
    return summary
  } catch (error) {
    const retryable = !(error instanceof SyncTransportError) || error.retryable
    if (retryable) {
      // Put the batch back so the next attempt sends it again, unchanged.
      for (const item of items) {
        await markFailedThenPending(item.clientId)
      }
      failures += 1
      setState(navigator.onLine ? 'syncing' : 'offline')
      scheduleRetry()
    } else {
      for (const item of items) {
        await markFailed(item.clientId, error instanceof Error ? error.message : 'Sync failed')
      }
      setState('synced')
    }
    await refreshCounts()
    return emptySummary
  }
}

async function markFailedThenPending(clientId: string): Promise<void> {
  await offlineDb.outbox.where('clientId').equals(clientId).modify({ status: 'pending' })
}

function onOutboxChanged(): void {
  failures = 0
  void syncNow()
}

function onOnline(): void {
  setOnline(true)
  void syncNow()
}

function onOffline(): void {
  setOnline(false)
  setState('offline')
}


export function startSyncRunner(): void {
  setOnline(navigator.onLine)
  window.addEventListener('online', onOnline)
  window.addEventListener('offline', onOffline)
  window.addEventListener(OUTBOX_CHANGED, onOutboxChanged)
  window.clearInterval(poller)
  poller = window.setInterval(() => {
    void syncNow()
  }, POLL_MS)
  void syncNow()
}

export function stopSyncRunner(): void {
  window.removeEventListener('online', onOnline)
  window.removeEventListener('offline', onOffline)
  window.removeEventListener(OUTBOX_CHANGED, onOutboxChanged)
  window.clearTimeout(timer)
  window.clearInterval(poller)
  poller = undefined
}

/** Re-read the outbox counters - for a screen that mounts after some work happened. */
export const recheckCounts = refreshCounts

export { subscribe as subscribeToSync }