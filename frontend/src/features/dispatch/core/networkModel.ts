import type { LiveBoard } from './api'

type TripState = 'Planned' | 'On road' | 'Completed'
export interface DistrictRow { district: string; trips: number; stops: number; done: number; states: Record<TripState, number> }

const stateFor = (status: string): TripState => status === 'COMPLETED' ? 'Completed'
  : status === 'ON_THE_WAY' ? 'On road' : 'Planned'

export function summarizeDistricts(trips: LiveBoard['trips']): DistrictRow[] {
  const rows = new Map<string, DistrictRow>()
  for (const trip of trips) {
    const row = rows.get(trip.district) ?? { district: trip.district, trips: 0, stops: 0, done: 0, states: { Planned: 0, 'On road': 0, Completed: 0 } }
    row.trips++
    row.stops += trip.stopsTotal
    row.done += trip.stopsDone
    row.states[stateFor(trip.status)]++
    rows.set(trip.district, row)
  }
  return [...rows.values()].sort((a, b) => a.district.localeCompare(b.district))
}
