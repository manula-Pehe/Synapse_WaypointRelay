import type { Order, Plan } from './api'

type TripState = 'Draft' | 'Planned' | 'On road' | 'Completed'
interface DistrictRow { district: string; trips: number; stops: number; states: Record<TripState, number> }

function tripState(plan: Plan, ids: string[], byId: Map<string, Order>): TripState {
  if (plan.status === 'DRAFT') return 'Draft'
  const statuses = ids.map(id => byId.get(id)?.status)
  if (statuses.length && statuses.every(status => ['DELIVERED', 'PARTIAL', 'FAILED'].includes(status ?? ''))) return 'Completed'
  if (statuses.some(status => ['ON_THE_WAY', 'DELIVERED', 'PARTIAL', 'FAILED'].includes(status ?? ''))) return 'On road'
  return 'Planned'
}

export function summarizeDistricts(plan: Plan, orders: Order[]): DistrictRow[] {
  const byId = new Map(orders.map(order => [order.id, order]))
  const rows = new Map<string, DistrictRow>()
  for (const vehicle of plan.vehicles) for (const trip of vehicle.trips) {
    const row = rows.get(trip.district) ?? { district: trip.district, trips: 0, stops: 0, states: { Draft: 0, Planned: 0, 'On road': 0, Completed: 0 } }
    row.trips++
    row.stops += trip.stops.length
    row.states[tripState(plan, trip.stops.map(stop => stop.orderId), byId)]++
    rows.set(trip.district, row)
  }
  return [...rows.values()].sort((a, b) => a.district.localeCompare(b.district))
}

