// Driver-side shapes. They mirror; the mock below is in the same shapes so a screen
// does not change when the real endpoints land.

export type DeliveryOutcome = 'DELIVERED' | 'PARTIAL' | 'FAILED'

export type DeliveryReason = 'STORE_CLOSED' | 'NO_ACCESS' | 'REFUSED' | 'DAMAGED'

export interface DriverOutlet {
  id: string
  /** Shown on the stop card; the server sends it per stop, so it lives beside the window. */
  name?: string
  brand: string
  district: string
  dockType: 'street' | 'rear_dock' | 'mall_bay'
  windowOpen: string
  windowClose: string
  note?: string
}

export interface DriverStop {
  id: string
  sequence: number
  outlet: DriverOutlet
  orderRef: string
  /** The order this stop delivers. Proof and outcomes are recorded against it, not the stop. */
  orderId?: string
  cases: number
  chilled: boolean
  /** Minutes before the window opens - the driver waits rather than unload into a shut shop. */
  earlyByMinutes: number
  predictedArrival: string
  status: 'PENDING' | 'ARRIVED' | 'DONE' | 'FAILED' | 'DEFERRED'
}

export interface DriverTrip {
  id: string
  no: string
  vehicleId: string
  depot: string
  stops: DriverStop[]
  /** What the loader counted, so the driver can check the load before accepting. */
  loadedCases: number
  fridgeTempC: number | null
  loadAccepted: boolean
}

export interface DriverRun {
  runDate: string
  vehicleId: string
  vehicleType: string
  trip: DriverTrip
}

export const ACCESS_NOTE: Record<DriverOutlet['dockType'], string> = {
  street: 'Street - park where you can',
  rear_dock: 'Rear dock - ring the bell',
  mall_bay: 'Mall bay - collect a pass at the desk',
}