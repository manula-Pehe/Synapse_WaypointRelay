import { useSyncExternalStore } from 'react'
import { getSnapshot, subscribe, type SyncSnapshot } from './store'

/**
 * The sync pill's data source. Mount it on every driver screen 
 */
export function useSyncStatus(): SyncSnapshot {
  return useSyncExternalStore(subscribe, getSnapshot)
}

/**
 * key for the pill text, so the driver sees
 * `Synced` · `Syncing` · `Offline · 3 waiting` 
 * Keys are returned rather than words because all driver text goes through 
 */
export function useSyncPillLabel(): { key: string; values?: Record<string, string | number> } {
  const { state, waiting } = useSyncStatus()

  if (state === 'offline') return { key: 'driver.sync.offline', values: { count: waiting } }
  if (state === 'syncing') return { key: 'driver.sync.syncing', values: { count: waiting } }
  if (waiting > 0) return { key: 'driver.sync.waiting', values: { count: waiting } }
  return { key: 'driver.sync.synced' }
}