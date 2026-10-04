import type { DriverOutlet } from './types'

/**
 * What the driver reads for a store.
 *
 * The server sends the store's name with each stop (docs/api.md §8, `/api/driver/today`), so that is
 * what a driver sees. `OUT-014` alone is not something to read at a glance in a cab, and it is the
 * one thing F12 cannot translate - a name is a name in Sinhala and Tamil too.
 */
export function outletLabel(outlet: DriverOutlet): string {
  return outlet.name?.trim() || outlet.id
}
