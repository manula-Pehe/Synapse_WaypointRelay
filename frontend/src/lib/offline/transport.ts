import type { SyncRequest, SyncRequestItem, SyncResponse } from './types'

export const SYNC_URL = '/api/sync'

/**
 * A failed sync. `retryable` is false when the server rejected the batch for a
 * reason another attempt will not fix (a 4xx) - the sync runner stops looping
 * and shows the driver what went wrong instead.
 */
export class SyncTransportError extends Error {
  readonly retryable: boolean

  constructor(message: string, retryable: boolean) {
    super(message)
    this.name = 'SyncTransportError'
    this.retryable = retryable
  }
}

export type SyncTransport = (items: SyncRequestItem[]) => Promise<SyncResponse>

/**
 * Default transport.
 *
 * NOTE: this is the only place in the driver app that calls fetch directly, so
 * that swapping in the shared `lib/api` client (Shaanil's app shell) is a
 * one-file change. Pass your own transport to syncRunner/syncNow to override it.
 */
export const defaultSyncTransport: SyncTransport = async (items) => {
  const body: SyncRequest = { items }

  let response: Response
  try {
    response = await fetch(SYNC_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
  } catch {
    // No route to the server - the common case for a driver in the hills.
    throw new SyncTransportError('No connection', true)
  }

  if (!response.ok) {
    const retryable = response.status >= 500 || response.status === 408 || response.status === 429
    throw new SyncTransportError(`Sync failed (${response.status})`, retryable)
  }

  return (await response.json()) as SyncResponse
}