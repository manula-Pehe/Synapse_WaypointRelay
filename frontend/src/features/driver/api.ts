import { useQuery } from '@tanstack/react-query'
import { api } from '../../lib/api'
import type { DriverStop, DriverTrip } from './types'

/**
 * The driver app's own endpoints (F3, F5, F10).
 *
 * Every *read* the screens need is one call - `today()` covers R1, R2 and R0 - so a run is one
 * request at sign-in rather than one per screen. Writes do not appear here: those go through the
 * outbox in `lib/offline`, which is the rule that lets the app work with no signal.
 */

/** As the server sends them (driver/dto). These are not what the screens render. */
interface StopPayload {
  id: string
  seq: number
  orderId: string
  orderRef: string
  outletId: string
  outletName: string
  district: string
  dockType: string
  windowOpen: string
  windowClose: string
  units: number
  temp: string
  earlyByMinutes: number
  reassigned: boolean
}

interface TripPayload {
  id: string
  tripNo: number
  brand: string
  district: string
  departAt: string
  stops: StopPayload[]
}

export interface TodayPayload {
  runDate: string
  vehicleId: string
  vehicleType: string
  loadedCases: number | null
  loadAccepted: boolean
  trips: TripPayload[]
}

export interface VehicleProblemPayload {
  id: string
  kind: string
  note: string | null
  canDrive: boolean
  fridgeTempC: number | null
  /** Cases stranded on the vehicle; null when the driver did not say. What D6b re-plans against. */
  unitsOnBoard: number | null
  status: string
  reply: string | null
  reportedAt: string
}

/** The three dock types planning knows; anything else reads as a street stop. */
function dockType(value: string): DriverStop['outlet']['dockType'] {
  return value === 'rear_dock' || value === 'mall_bay' ? value : 'street'
}

function toStop(payload: StopPayload): DriverStop {
  return {
    id: payload.id,
    sequence: payload.seq,
    orderId: payload.orderId,
    orderRef: payload.orderRef,
    cases: payload.units,
    chilled: payload.temp === 'CHILLED',
    earlyByMinutes: payload.earlyByMinutes,
    predictedArrival: '',
    status: payload.reassigned ? 'DEFERRED' : 'PENDING',
    outlet: {
      id: payload.outletId,
      name: payload.outletName,
      brand: '',
      district: payload.district,
      dockType: dockType(payload.dockType),
      windowOpen: payload.windowOpen,
      windowClose: payload.windowClose,
    },
  }
}

/**
 * Folds the server's run into the single-trip shape the screens take.
 *
 * The screens were built against one trip, and the plan for a vehicle is one run of ordered stops;
 * flattening here keeps every screen and the offline cache unchanged.
 */
function toTrip(payload: TodayPayload, trip: TripPayload | undefined): DriverTrip {
  const source = trip ?? payload.trips[0]
  return {
    id: source?.id ?? '',
    no: source ? `Trip ${source.tripNo}` : 'No trip',
    vehicleId: payload.vehicleId,
    depot: source?.district ?? '',
    stops: (source?.stops ?? []).map(toStop),
    loadedCases: payload.loadedCases ?? 0,
    fridgeTempC: null,
    loadAccepted: payload.loadAccepted,
  }
}

/** As `driver/dto/ConflictDto` sends it. `details` carries the reason and both sides of the clash. */
export interface ConflictPayload {
  id: string
  stopId: string
  orderId: string
  deliveryId: string | null
  details: Record<string, unknown>
  createdAt: string
}

export interface FailedDeliveryPayload {
  id: string
  stopId: string
  orderId: string
  vehicleId: string
  outcome: string
  units: number
  reason: string | null
  receivedBy: string | null
  hasPhoto: boolean
  hasSignature: boolean
  completedAt: string
  undone: boolean
  /** Set once dispatch has settled it; `storeChoice` once the store has answered. */
  decision: string | null
  storeChoice: string | null
}

/** Every list endpoint on this server answers in the `{ items, total }` envelope. */
interface List<T> {
  items: T[]
  total: number
}

/** D8 - one card per sync clash. `KEEP_FIELD` keeps what the driver recorded. */
export const dispatchApi = {
  conflicts: (runDate?: string) =>
    api<List<ConflictPayload>>(`dispatch/conflicts${runDate ? `?runDate=${runDate}` : ''}`),

  resolveConflict: (id: string, keepField: 'KEEP_FIELD' | 'OVERRIDE') =>
    api<ConflictPayload>(`dispatch/conflicts/${encodeURIComponent(id)}/resolve`, {
      method: 'POST',
      body: JSON.stringify({ keepField }),
    }),

  /** D6f - failed stops waiting on a decision. */
  failed: (runDate?: string) =>
    api<List<FailedDeliveryPayload>>(`dispatch/failed${runDate ? `?runDate=${runDate}` : ''}`),

  decide: (id: string, decision: 'REPLAN_TOMORROW' | 'TRY_LATER_TODAY' | 'CANCEL') =>
    api<FailedDeliveryPayload>(`dispatch/failed/${encodeURIComponent(id)}/decide`, {
      method: 'POST',
      body: JSON.stringify({ decision }),
    }),

  /** F10 - the problems drivers have raised, so dispatch can answer them. */
  problems: () => api<List<VehicleProblemPayload>>('dispatch/problems'),

  replyProblem: (id: string, text: string) =>
    api<VehicleProblemPayload>(`dispatch/problems/${encodeURIComponent(id)}/reply`, {
      method: 'POST',
      body: JSON.stringify({ text }),
    }),
}

export const driverApi = {
  /** R1/R2/R0 - one call for the whole run, so it is the one thing cached for offline use. */
  today: () => api<TodayPayload>('driver/today'),

  /**
   * R0 - accepts the load. This is the one write that goes straight to the server: it is a
   * decision about a truck the driver is looking at, it is made once, and a queued copy would
   * leave the screen disagreeing with the server after a sync.
   */
  acceptTrip: (tripId: string) =>
    api<TodayPayload>(`driver/trips/${encodeURIComponent(tripId)}/accept`, { method: 'POST' }),

  /** R9 - a problem report. Queued through the outbox like every other driver write. */
  problems: () => api<List<VehicleProblemPayload>>('driver/problems'),

  /** F5 - proof upload. Multipart, and idempotent on clientId so a retry is not a second file. */
  uploadProof: (file: File, kind: 'photo' | 'signature', clientId: string) => {
    const body = new FormData()
    body.append('file', file)
    body.append('kind', kind)
    body.append('clientId', clientId)
    return api<{ id: string }>('driver/files', { method: 'POST', body })
  },
}

export { toStop, toTrip }

/**
 * R9ok - dispatch's most recent instruction on this driver's own problems, or null when there is
 * nothing to read. Returns a string rather than the row because that is all the screen shows.
 */
export function useDriverReply(): string | null {
  const { data } = useQuery({
    queryKey: ['driver', 'problems'],
    queryFn: () => driverApi.problems(),
    staleTime: 60_000,
    retry: false,
  })
  const answered = (data?.items ?? []).filter((problem) => problem.reply)
  return answered.length > 0 ? answered[0].reply : null
}
