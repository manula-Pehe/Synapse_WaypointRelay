/**
 * lib/offline - the driver's outbox, sync runner and offline cache.
 *
 * See README.md in this folder for the API the loader app reuses.
 */
export type {
  JsonObject,
  JsonValue,
  OutboxRow,
  OutboxStatus,
  SyncRequest,
  SyncRequestItem,
  SyncResponse,
  SyncResponseItem,
  SyncResult,
  SyncState,
  SyncSummary,
  SyncType,
} from './types'
export { SYNC_RESULTS, SYNC_TYPES } from './types'

export { offlineDb, type CacheKey, type CacheRow } from './db'

export {
  countWaiting,
  enqueue,
  history,
  markFailed,
  markResults,
  markSending,
  pruneDone,
  refreshCounts,
  toRequestItem,
  unsentItems,
} from './outbox'

export {
  OUTBOX_CHANGED,
  recheckCounts,
  setSyncTransport,
  startSyncRunner,
  stopSyncRunner,
  syncNow,
} from './syncRunner'

export { defaultSyncTransport, SyncTransportError, SYNC_URL, type SyncTransport } from './transport'

export { useSyncPillLabel, useSyncStatus } from './useSyncStatus'

export {
  checkPinOffline,
  forgetPin,
  rememberPin,
  type OfflineCheck,
  type OfflineCredential,
} from './pin'