import { api, ApiError } from '../api'
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
 * Goes through the shared `lib/api` client rather than `fetch`, so the batch carries the driver's
 * bearer token like every other call. That matters for X1m-off: a session minted offline has no
 * token, and the work must stay queued in the outbox until a real sign-in puts one back, rather
 * than being sent and rejected.
 *
 * Pass your own transport to syncRunner/syncNow to override this.
 */
export const defaultSyncTransport: SyncTransport = async (items) => {
  const body: SyncRequest = { items }

  try {
    return await api<SyncResponse>('sync', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  } catch (cause) {
    if (cause instanceof ApiError) {
      // 401/403 will not fix themselves on a retry; the driver has to sign in online again. 5xx and
      // throttling are worth another attempt.
      const retryable = cause.status >= 500 || cause.status === 408 || cause.status === 429
      throw new SyncTransportError(`Sync failed (${cause.status})`, retryable)
    }
    // No route to the server - the common case for a driver in the hills.
    throw new SyncTransportError('No connection', true)
  }
}