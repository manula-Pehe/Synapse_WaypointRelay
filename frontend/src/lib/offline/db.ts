import Dexie, { type EntityTable } from 'dexie'
import type { OutboxRow } from './types'

/* Downloaded reference data, so the driver's screens work without signal. */
export interface CacheRow {
  key: string
  data: unknown
  fetchedAt: string
}

export type CacheKey = 'today' | 'outlets' | 'i18n'


class OfflineDb extends Dexie {
  outbox!: EntityTable<OutboxRow, 'seq'>
  cache!: EntityTable<CacheRow, 'key'>

  constructor() {
    super('waypoint-offline')
    this.version(1).stores({
      outbox: '++seq, &clientId, status, type',
      cache: '&key',
    })
  }
}

export const offlineDb = new OfflineDb()