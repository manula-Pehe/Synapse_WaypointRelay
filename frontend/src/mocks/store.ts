import { ApiError } from '../lib/api'
import type { Delivery, Issue, Order, OrderDetail, OrderEvent, StoreHome } from '../features/store/api'

interface DemoState { orders: Order[]; events: Record<string, OrderEvent[]>; issues: Issue[]; receipts: Record<string, { receivedUnits: number; at: string; note: string }>; choices: Record<string, string>; photos: Record<string, string> }
let memory: DemoState | null = null
const initialTime = '2026-09-30T14:00:00+05:30'
const run = '2026-10-01'
const yesterday = '2026-09-30'
function makeOrder(id: string, status: Order['status'], units: number, date = run, source = 'SEED', storeChecked = true): Order {
  return { id, ref: `DEMO-${id.split('-').at(-1)}`, outletId: 'OUT001', outletName: 'Demo outlet', brand: 'Fresh', temp: 'AMBIENT', units, runDate: date, status, source, storeChecked, updatedAt: initialTime, confirmedAt: status === 'PREPARED' ? null : initialTime, weightKg: units * 10, volumeM3: units / 10 }
}
function fresh(): DemoState {
  return { orders: [makeOrder('demo-1','PREPARED',24), makeOrder('demo-2','CONFIRMED',18,run,'PHONE_IN',false), makeOrder('demo-3','PLANNED',30), makeOrder('demo-4','MOVED',14,'2026-10-02'), makeOrder('demo-5','DELIVERED',20,yesterday), makeOrder('demo-6','PARTIAL',16,yesterday), makeOrder('demo-7','FAILED',12,yesterday)], events: {}, issues: [], receipts: {}, choices: {}, photos: {} }
}
function state(): DemoState {
  try { const value = localStorage.getItem('waypoint.demo.store.v1'); return value ? JSON.parse(value) as DemoState : memory ?? fresh() } catch { return memory ?? fresh() }
}
function save(next: DemoState) { memory = next; try { localStorage.setItem('waypoint.demo.store.v1', JSON.stringify(next)) } catch { /* Session memory remains available. */ } }
function findOrder(next: DemoState, id: string) { const order = next.orders.find(o => o.id === id); if (!order) throw new ApiError(404, 'NOT_FOUND', 'Order not found.'); return order }
function updateOrder(id: string, change: (order: Order) => Order, type: string): Order {
  const next = state(), current = findOrder(next, id), updated = change(current)
  next.orders = next.orders.map(order => order.id === id ? updated : order)
  next.events[id] = [...(next.events[id] ?? []), { at: initialTime, actor: 'demo-store', type, fromStatus: current.status, toStatus: updated.status, details: {} }]
  save(next); return updated
}
function mutationAllowed(order: Order) { if (order.runDate === run && state().choices.closed === 'true') throw new ApiError(409, 'ORDERS_CLOSED', 'Orders for this run are closed.'); if (order.status !== 'PREPARED') throw new ApiError(409, 'INVALID_STATUS', 'Only prepared orders can be changed.') }
function deliveryFor(order: Order, next: DemoState): Delivery {
  const plan = ['PLANNED','LOADED','ON_THE_WAY'].includes(order.status)
  const delivered = ['DELIVERED','PARTIAL','FAILED'].includes(order.status)
  return { orderId: order.id, orderRef: order.ref, status: order.status,
    arrival: plan ? { from: `${order.runDate}T05:00:00+05:30`, to: `${order.runDate}T05:40:00+05:30`, vehicleId: 'DEMO-VAN' } : null,
    deferral: order.status === 'MOVED' ? { id: order.id, reason: 'Vehicle capacity', newDate: order.runDate, splitOffered: true } : null,
    delivery: delivered ? { id: order.id, outcome: order.status, units: order.status === 'PARTIAL' ? 12 : order.status === 'FAILED' ? 0 : order.units, receivedBy: order.status === 'FAILED' ? undefined : 'Demo manager', at: initialTime } : null,
    shortfall: order.status === 'PARTIAL' ? { missingUnits: 4, reason: 'Cases unavailable at loading' } : null,
    breakdown: order.status === 'FAILED' ? { stopId: order.id, reason: 'Vehicle unavailable' } : null,
    driverStatus: plan ? { offline: false } : null, receipt: next.receipts[order.id] ?? null }
}
function issue(next: DemoState, id: string) { const value = next.issues.find(i => i.id === id); if (!value) throw new ApiError(404, 'NOT_FOUND', 'Issue not found.'); return value }
function updateIssue(id: string, change: (value: Issue) => Issue) { const next = state(), value = change(issue(next,id)); next.issues = next.issues.map(item => item.id === id ? value : item); save(next); return value }
function addMessage(value: Issue, text: string, authorId: string, authorName: string): Issue { return { ...value, messages: [...value.messages, { id: crypto.randomUUID(), authorId, authorName, text, createdAt: new Date().toISOString() }] } }
const list = <T,>(items: T[]) => ({ items, total: items.length })
export const demoStoreApi = {
  home: async (): Promise<StoreHome> => { const next = state(); return { outlet: 'OUT001', brand: 'Fresh', runDate: run, now: initialTime, ordersClosed: next.choices.closed === 'true', cutOffAt: '2026-09-30T16:00:00+05:30', tomorrow: next.orders.filter(o => o.runDate === run), today: next.orders.filter(o => o.runDate === yesterday), openIssues: next.issues.filter(i => i.status !== 'RESOLVED').length } },
  orders: async (from?: string, to?: string) => list(state().orders.filter(o => (!from || o.runDate >= from) && (!to || o.runDate <= to))),
  order: async (id: string): Promise<OrderDetail> => { const next = state(); return { order: findOrder(next,id), history: next.events[id] ?? [] } },
  edit: async (id: string, units: number) => updateOrder(id, order => { mutationAllowed(order); if (units < 1) throw new ApiError(400, 'VALIDATION', 'Cases must be positive.'); return { ...order, units } }, 'EDITED'),
  confirm: async (id: string) => updateOrder(id, order => { mutationAllowed(order); return { ...order, status: 'CONFIRMED', confirmedAt: initialTime } }, 'CONFIRMED'),
  cancel: async (id: string) => updateOrder(id, order => { mutationAllowed(order); return { ...order, status: 'CANCELLED' } }, 'CANCELLED'),
  create: async (body: { runDate: string; temp: 'CHILLED' | 'AMBIENT'; units: number; note: string }) => { const next = state(); const id = `demo-${crypto.randomUUID()}`; const order = { ...makeOrder(id,'PREPARED',body.units,body.runDate,'STORE'), temp: body.temp }; next.orders.unshift(order); next.events[id] = [{ at: initialTime, actor: 'demo-store', type: 'PREPARED', fromStatus: null, toStatus: 'PREPARED', details: { note: body.note } }]; save(next); return order },
  check: async (id: string, ok: boolean, message?: string) => updateOrder(id, order => { if (order.source !== 'PHONE_IN' || order.storeChecked) throw new ApiError(409, 'INVALID_STATUS', 'This phone order has already been checked.'); if (!ok && !message?.trim()) throw new ApiError(400, 'VALIDATION', 'Tell dispatch what is wrong.'); return { ...order, storeChecked: true } }, ok ? 'STORE_CHECKED' : 'DISPUTED'),
  deliveries: async (runDate?: string) => { const next = state(), date = runDate ?? yesterday; return list(next.orders.filter(o => o.runDate === date && ['CONFIRMED','PLANNED','LOADED','ON_THE_WAY','DELIVERED','PARTIAL','FAILED','MOVED'].includes(o.status)).map(o => deliveryFor(o,next))) },
  receipt: async (id: string, receivedUnits: number, note: string) => { const next = state(), order = findOrder(next,id); if (!['DELIVERED','PARTIAL'].includes(order.status)) throw new ApiError(409, 'INVALID_STATUS', 'Order not delivered.'); if (next.receipts[id]) throw new ApiError(409, 'DUPLICATE', 'Already received.'); next.receipts[id] = { receivedUnits, at: new Date().toISOString(), note }; save(next); return next.receipts[id] },
  deferralChoice: async (id: string, choice: string, units?: number) => { const next = state(); next.choices[`deferral:${id}`] = `${choice}:${units ?? ''}`; save(next); return { choice } },
  failedChoice: async (id: string, choice: string) => { const next = state(); next.choices[`failed:${id}`] = choice; save(next); return { choice } },
  breakdownChoice: async (id: string, accept: boolean) => { const next = state(); next.choices[`breakdown:${id}`] = String(accept); save(next); return { accept } },
  issues: async () => list(state().issues),
  issue: async (id: string) => issue(state(),id),
  createIssue: async (body: { orderId: string | null; type: string; units: number | null; wants: string; note: string }): Promise<Issue> => { const next = state(), id = crypto.randomUUID(); const value: Issue = { id, ref: `ISS-${id.slice(0,8).toUpperCase()}`, outletId: 'OUT001', orderId: body.orderId, type: body.type, units: body.units, wants: body.wants, status: 'OPEN', createdAt: new Date().toISOString(), resolvedAt: null, messages: [], photoIds: [] }; next.issues.unshift(addMessage(value,body.note,'demo-store','Dilani')); save(next); return next.issues[0] },
  issueMessage: async (id: string, text: string) => updateIssue(id, value => addMessage(value,text,'demo-store','Dilani')),
  uploadPhoto: async (id: string, file: File) => { const next = state(), value = issue(next,id), photoId = crypto.randomUUID(); const data = await new Promise<string>((resolve,reject) => { const reader = new FileReader(); reader.onload = () => resolve(String(reader.result)); reader.onerror = reject; reader.readAsDataURL(file) }); next.photos[photoId] = data; next.issues = next.issues.map(i => i.id === id ? { ...value, photoIds: [...value.photoIds,photoId] } : i); save(next); return { id: photoId, url: data } },
  issuePhoto: async (_id: string, photoId: string) => { const data = state().photos[photoId]; if (!data) throw new ApiError(404,'NOT_FOUND','Photo not found.'); return fetch(data).then(response => response.blob()) },
  dispatchIssues: async (status?: string) => list(state().issues.filter(i => !status || i.status === status)),
  dispatchReply: async (id: string, text: string) => updateIssue(id, value => ({ ...addMessage(value,text,'demo-dispatch','Ruwan'), status: 'ANSWERED' })),
  dispatchResolve: async (id: string) => updateIssue(id, value => ({ ...value, status: 'RESOLVED', resolvedAt: new Date().toISOString() })),
}
