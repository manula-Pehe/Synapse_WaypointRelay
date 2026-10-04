import { api } from '../../../lib/api'

export interface List<T> { items: T[]; total: number }
export interface Settings { runDate: string; now: string; timezone: string }
export interface Order {
  id: string; ref: string; outletId: string; outletName: string; brand: string
  temp: 'CHILLED' | 'AMBIENT'; units: number; weightKg: number; volumeM3: number
  runDate: string; status: string; source: string; daysSinceLastServed: number
  deferredYesterday: boolean; updatedAt: string
}
export interface OrderEvent { at: string; actor: string | null; type: string; fromStatus: string | null; toStatus: string | null; details: Record<string, unknown> }
export interface OrderDetail { order: Order; history: OrderEvent[] }
export interface UnconfirmedOutlet { outletId: string; outletName: string; phone: string | null; orders: Order[] }
export interface CloseStatus { closed: boolean; closedAt: string | null; closedBy: string | null; cutOffAt: string }
export interface CloseResult { closedAt: string; confirmed: number; autoConfirmed: number; notConfirmed: number }
export interface Vehicle {
  id: string; type: string; temp: string; weightCapKg: number; volumeCapM3: number
  fuelType: string; kmPerL: number; weeklyFuelQuotaL: number; depot: string
  availability: 'AVAILABLE' | 'IN_WORKSHOP' | 'OFF_ROAD'; availabilityReason: string | null
}
export interface Fleet { items: Vehicle[]; confirmedAt: string | null; confirmedBy: string | null; counts: { available: number; inWorkshop: number; offRoad: number; reeferAvailable: number } }
export interface Outlet { id: string; name: string; brand: string; district: string; depot: string; dockType: string; parkingConstraint: string; windowOpen: string; windowClose: string; mallWindowOpen: string | null; mallWindowClose: string | null }
export interface Notice { id: string; severity: 'CRITICAL' | 'WARNING' | 'INFO'; type: string; title: string; body: string; link: string | null; createdAt: string; readAt: string | null }
export interface NoticeList extends List<Notice> { unreadCount: number }
export interface PlanReadiness { ordersClosed: boolean; fleetConfirmed: boolean; confirmedOrders: number; availableVehicles: number; reeferAvailable: number; warnings: string[] }
export interface PlanStop { id: string; orderId: string; orderRef: string; outletId: string; seq: number; loadSeq: number; units: number; temp: string; arriveFrom: string; arriveTo: string; lateRisk: number | null }
export interface PlanTrip { id: string; tripNo: number; brand: string; district: string; windowType: string; departAt: string; minutes: number; weightKg: number; volumeM3: number; stops: PlanStop[] }
export interface PlanVehicle { vehicleId: string; type: string; temp: string; weightCapKg: number; volumeCapM3: number; freshMinutesUsed: number; freshBudget: number; daytimeMinutesUsed: number; daytimeBudget: number; trips: PlanTrip[] }
export interface PlanSummary { served: number; deferred: number; unavoidable: number; chosen: number; violations: number; fridgeVehiclesUsed: number; fridgeVehiclesAvailable: number; warnings: number }
export interface Plan { id: string; runDate: string; depot: string; version: number; status: 'DRAFT' | 'PUBLISHED' | 'SUPERSEDED'; summary: PlanSummary; vehicles: PlanVehicle[] }
export interface PlanDeferral { id: string; orderId: string; orderRef: string; outletId: string; kind: 'UNAVOIDABLE' | 'CHOSEN'; rule: string; reason: string; priorityScore: number; daysWaited: number; newDate: string; needsDecision: boolean; storeChoice: string | null }
export interface LiveBoard {
  runDate: string; depot: string; generatedAt: string; onTimePercent: number | null
  completedStops: number; onTimeStops: number; deferredToday: number | null
  skippedTwoRuns: number | null; fridgeTruckUsePercent: number | null
  needsAttention: { id: string; type: string; severity: 'CRITICAL' | 'WARNING'; title: string; details: string; orderId: string | null; action: string | null }[]
  trips: { id: string; vehicleId: string; tripNo: number; district: string; stopsDone: number; stopsTotal: number; status: string; lastUpdate: string | null; lastSync: string | null }[]
}
export interface RunReport {
  runDate: string; depot: string; onTimePercent: number | null; onTimeStops: number
  completedStops: number; deferred: number | null; failed: number; partial: number
  lateByDistrict: { district: string; late: number; completed: number }[]
  exceptions: { type: string; detail: string; orderId: string }[]
}

const params = (values: Record<string, string>) => new URLSearchParams(values).toString()
const json = (body: unknown) => JSON.stringify(body)

export const dispatchApi = {
  settings: () => api<Settings>('settings'),
  moveClock: (at: string) => api<Settings>('settings/clock', { method: 'POST', body: json({ at }) }),
  orders: (runDate: string, depot: string) => api<List<Order>>(`orders?${params({ runDate, depot })}`),
  unconfirmed: (runDate: string, depot: string) => api<List<UnconfirmedOutlet>>(`dispatch/orders/unconfirmed?${params({ runDate, depot })}`),
  closeStatus: (runDate: string, depot: string) => api<CloseStatus>(`orders/close-status?${params({ runDate, depot })}`),
  closeOrders: (runDate: string, depot: string) => api<CloseResult>('dispatch/orders/close', { method: 'POST', body: json({ runDate, depot }) }),
  order: (id: string) => api<OrderDetail>(`orders/${encodeURIComponent(id)}`),
  phoneIn: (body: { outletId: string; runDate: string; temp: 'CHILLED' | 'AMBIENT'; units: number; note: string }) => api<Order>('dispatch/orders/phone-in', { method: 'POST', body: json(body) }),
  fleet: (runDate: string, depot: string) => api<Fleet>(`dispatch/fleet?${params({ runDate, depot })}`),
  setAvailability: (id: string, runDate: string, status: Vehicle['availability'], reason: string | null) => api<Vehicle>(`dispatch/fleet/${encodeURIComponent(id)}`, { method: 'PUT', body: json({ runDate, status, reason }) }),
  confirmFleet: (runDate: string, depot: string) => api<{ confirmedAt: string; confirmedBy: string }>('dispatch/fleet/confirm', { method: 'POST', body: json({ runDate, depot }) }),
  outlets: (depot: string) => api<List<Outlet>>(`outlets?${params({ depot })}`),
  notifications: () => api<NoticeList>('notifications'),
  markNotificationRead: (id: string) => api<Notice>(`notifications/${encodeURIComponent(id)}/read`, { method: 'POST' }),
  markAllNotificationsRead: () => api<{ updated: number }>('notifications/read-all', { method: 'POST' }),
  latestPlan: (runDate: string, depot: string) => api<Plan>(`dispatch/plans?${params({ runDate, depot })}`),
  planReadiness: (runDate: string, depot: string) => api<PlanReadiness>(`dispatch/plans/readiness?${params({ runDate, depot })}`),
  createPlan: (runDate: string, depot: string) => api<Plan>('dispatch/plans', { method: 'POST', body: json({ runDate, depot }) }),
  planDeferrals: (id: string) => api<List<PlanDeferral>>(`dispatch/plans/${encodeURIComponent(id)}/deferrals`),
  publishPlan: (id: string) => api<Plan>(`dispatch/plans/${encodeURIComponent(id)}/publish`, { method: 'POST' }),
  live: (runDate: string, depot: string) => api<LiveBoard>(`dispatch/live?${params({ runDate, depot })}`),
  runReport: (runDate: string, depot: string) => api<RunReport>(`dispatch/reports/run?${params({ runDate, depot })}`),
}
