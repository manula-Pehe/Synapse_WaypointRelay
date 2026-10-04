import { api } from '../../lib/api'

export interface TripSummary {
  tripId: string
  vehicleId: string
  tripNo: number
  district: string
  brand: string
  stops: number
  units: number
  chilled: boolean
  departAt: string
  status: 'WAITING' | 'LOADING' | 'READY' | 'LOADED' | 'DEPARTED'
  ticked: number
  vehicleAvailable: boolean
}

export interface StopDetail {
  stopId: string
  loadSeq: number
  orderId: string
  orderRef: string
  outletId: string
  outletName: string
  units: number
  weightKg: number
  volumeM3: number
  accessNote: string
  ticked: boolean
  missingUnits: number
}

export interface FridgeCheck {
  running: boolean
  tempC: number
  doorsOk: boolean
  passed: boolean
  checkedAt: string
}

export interface TripDetail {
  trip: TripSummary
  vehicleType: string
  weightCapKg: number
  volumeCapM3: number
  loadedWeightKg: number
  loadedVolumeM3: number
  stops: StopDetail[]
  fridgeCheck: FridgeCheck | null
}

export const loaderApi = {
  trips: () =>
    api<{ items: TripSummary[]; total: number; listsAvailableAt: string }>('loader/trips'),
  trip: (id: string) => api<TripDetail>(`loader/trips/${encodeURIComponent(id)}`),
  fridgeCheck: (id: string, body: { running: boolean; tempC: number; doorsOk: boolean }) =>
    api<FridgeCheck>(`loader/trips/${encodeURIComponent(id)}/fridge-check`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  tick: (id: string) =>
    api<TripDetail>(`loader/stops/${encodeURIComponent(id)}/tick`, { method: 'POST' }),
  shortfall: (id: string, body: { missingUnits: number; reason: string; note: string }) =>
    api<{ remainderOrderRef: string }>(`loader/stops/${encodeURIComponent(id)}/shortfall`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  handover: (id: string, driverStaffId: string) =>
    api<{ status: string; at: string }>(`loader/trips/${encodeURIComponent(id)}/handover`, {
      method: 'POST',
      body: JSON.stringify({ driverStaffId }),
    }),
}
