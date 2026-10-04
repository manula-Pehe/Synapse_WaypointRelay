import { api, apiBlob } from '../../lib/api'

export type OrderStatus = 'PREPARED' | 'CONFIRMED' | 'PLANNED' | 'LOADED' | 'ON_THE_WAY' | 'DELIVERED' | 'PARTIAL' | 'FAILED' | 'MOVED' | 'CANCELLED'
export interface Order {
  id: string; ref: string; outletId: string; outletName: string; brand: string; temp: 'CHILLED' | 'AMBIENT'
  units: number; runDate: string; status: OrderStatus; source: string; storeChecked: boolean
  autoConfirm: boolean
  updatedAt: string; confirmedAt: string | null; weightKg: number; volumeM3: number
}
export interface OutletDetails { id: string; name: string; brand: string; district: string; depot: string; dockType: string; parkingConstraint: string; windowOpen: string; windowClose: string; mallWindowOpen: string | null; mallWindowClose: string | null }
export interface StoreNotificationSettings { deliveries: boolean; orders: boolean; issues: boolean }
export interface OrderEvent { at: string; actor: string | null; type: string; fromStatus: OrderStatus | null; toStatus: OrderStatus; details: Record<string, unknown> }
export interface OrderDetail { order: Order; history: OrderEvent[] }
export interface StoreHome { outlet: string; brand: string; runDate: string; now: string; ordersClosed: boolean; cutOffAt: string; tomorrow: Order[]; today: Order[]; openIssues: number }
export interface Delivery { orderId: string; orderRef: string; status: OrderStatus; arrival: { from: string; to: string; changedReason?: string; lateRisk?: number; vehicleId?: string } | null; deferral: { id?: string; reason: string; newDate: string; splitOffered?: boolean } | null; delivery: { id?: string; outcome: string; units: number; photoUrl?: string; signatureUrl?: string; receivedBy?: string; at?: string } | null; shortfall: { missingUnits: number; reason: string } | null; breakdown?: { stopId: string; reason: string } | null; driverStatus?: { offline: boolean; lastSyncAt?: string } | null; receipt: { receivedUnits: number; at: string } | null }
export interface IssueMessage { id: string; authorId: string; authorName: string; text: string; createdAt: string }
export interface Issue { id: string; ref: string; outletId: string; orderId: string | null; type: string; units: number | null; wants: string; status: 'OPEN' | 'ANSWERED' | 'RESOLVED'; createdAt: string; resolvedAt: string | null; messages: IssueMessage[]; photoIds: string[] }
interface List<T> { items: T[]; total: number }
const realStoreApi = {
  outlet: (id: string) => api<OutletDetails>(`outlets/${encodeURIComponent(id)}`),
  notificationSettings: () => api<StoreNotificationSettings>('store/notifications/settings'),
  saveNotificationSettings: (settings: StoreNotificationSettings) => api<StoreNotificationSettings>('store/notifications/settings', { method: 'PUT', body: JSON.stringify(settings) }),
  notificationReads: () => api<string[]>('store/notifications/reads'),
  markNotificationReads: (ids: string[]) => api<void>('store/notifications/reads', { method: 'POST', body: JSON.stringify({ ids }) }),
  home: () => api<StoreHome>('store/home'),
  orders: (from?: string, to?: string) => api<List<Order>>(`store/orders?${new URLSearchParams({ ...(from ? { from } : {}), ...(to ? { to } : {}) })}`),
  order: (id: string) => api<OrderDetail>(`store/orders/${id}`),
  edit: (id: string, units: number) => api<Order>(`store/orders/${id}`, { method: 'PUT', body: JSON.stringify({ units }) }),
  confirm: (id: string) => api<Order>(`store/orders/${id}/confirm`, { method: 'POST' }),
  cancel: (id: string) => api<Order>(`store/orders/${id}/cancel`, { method: 'POST' }),
  create: (body: { runDate: string; temp: 'CHILLED' | 'AMBIENT'; units: number; note: string }) => api<Order>('store/orders', { method: 'POST', body: JSON.stringify(body) }),
  check: (id: string, ok: boolean, message?: string) => api<Order>(`store/orders/${id}/check`, { method: 'POST', body: JSON.stringify({ ok, message }) }),
  deliveries: (runDate?: string) => api<List<Delivery>>(`store/deliveries${runDate ? `?runDate=${encodeURIComponent(runDate)}` : ''}`),
  receipt: (id: string, receivedUnits: number, note: string) => api(`store/orders/${id}/receipt`, { method: 'POST', body: JSON.stringify({ receivedUnits, note }) }),
  deferralChoice: (id: string, choice: 'KEEP' | 'REDUCE' | 'CANCEL' | 'SPLIT', units?: number) => api(`store/deferrals/${id}/choice`, { method: 'POST', body: JSON.stringify({ choice, ...(units === undefined ? {} : { units }) }) }),
  failedChoice: (id: string, choice: 'REPLAN_TOMORROW' | 'TRY_LATER_TODAY' | 'CANCEL') => api(`store/failed/${id}/choice`, { method: 'POST', body: JSON.stringify({ choice }) }),
  breakdownChoice: (stopId: string, accept: boolean) => api(`store/breakdown/${stopId}/choice`, { method: 'POST', body: JSON.stringify({ accept }) }),
  issues: () => api<List<Issue>>('store/issues'),
  issue: (id: string) => api<Issue>(`store/issues/${id}`),
  createIssue: (body: { orderId: string | null; type: string; units: number | null; wants: string; note: string }) => api<Issue>('store/issues', { method: 'POST', body: JSON.stringify(body) }),
  issueMessage: (id: string, text: string) => api<Issue>(`store/issues/${id}/messages`, { method: 'POST', body: JSON.stringify({ text }) }),
  uploadPhoto: (id: string, file: File) => { const body = new FormData(); body.append('file', file); return api<{ id: string; url: string }>(`store/issues/${id}/photos`, { method: 'POST', body }) },
  issuePhoto: (id: string, photoId: string) => apiBlob(`store/issues/${id}/photos/${photoId}`),
  dispatchIssues: (status?: string) => api<List<Issue>>(`dispatch/issues${status ? `?status=${status}` : ''}`),
  dispatchReply: (id: string, text: string) => api<Issue>(`dispatch/issues/${id}/reply`, { method: 'POST', body: JSON.stringify({ text }) }),
  dispatchResolve: (id: string) => api<Issue>(`dispatch/issues/${id}/resolve`, { method: 'POST' }),
}
export const storeApi = realStoreApi
