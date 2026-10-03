import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { storeApi, type Order } from './api'
import { Button, Card, Feedback, Heading, Loading, Status } from './StoreShared'

export function StoreHome() {
  const query = useQuery({ queryKey: ['store', 'home'], queryFn: storeApi.home, refetchInterval: 30_000 })
  const [offline, setOffline] = useState(!navigator.onLine)
  useEffect(() => { const update = () => setOffline(!navigator.onLine); window.addEventListener('online', update); window.addEventListener('offline', update); return () => { window.removeEventListener('online', update); window.removeEventListener('offline', update) } }, [])
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  const home = query.data
  const prepared = home.tomorrow.filter(order => order.status === 'PREPARED')
  const delivery = home.today.find(order => ['DELIVERED', 'PARTIAL'].includes(order.status)) ?? home.today[0]
  return <>
    {offline && <p role="status" className="mb-5 rounded-lg bg-warning-soft p-4 font-medium text-warning">◇ Offline · Changes are waiting for connection.</p>}
    <div className="store-home-grid">
      <div>
        <section className="store-focus-card">
          <div className="store-section-title"><h1>Tomorrow&apos;s orders · {formatStoreDate(home.runDate)}</h1>{prepared.length > 0 && <span className="store-pill store-pill-warning">⚠ {prepared.length} to confirm</span>}</div>
          <div className="store-order-stack">{home.tomorrow.length ? home.tomorrow.map(order => <Link className="store-order-row" to={`/store/orders/${order.id}`} key={order.id}><span className="store-order-icon" aria-hidden="true">{order.temp === 'CHILLED' ? '❄' : '⬡'}</span><span className="store-order-copy"><strong>{order.temp === 'CHILLED' ? 'Chilled' : 'Dry goods'} · {order.units} cases</strong><small>{order.status === 'PREPARED' ? 'Prepared from your order history · needs your confirmation' : order.status === 'CONFIRMED' ? 'Your order is confirmed' : 'Open order details'}</small></span><Status status={order.status} /></Link>) : <p className="store-empty">No order for this run yet.</p>}</div>
          {prepared.length > 0 ? <Link to={`/store/orders/${prepared[0].id}`} className="store-primary-action">⊙ Review and confirm</Link> : <Link to="/store/orders" className="store-primary-action">View all orders</Link>}
        </section>
        <section className="store-updates-card"><h2>Updates</h2><div className="store-update-row"><span className="store-pill store-pill-success">⊙ Confirmed</span><span>{home.tomorrow.find(order => order.status === 'CONFIRMED') ? 'Your order is confirmed for the next run' : 'Your next order is ready to review'}</span></div><div className="store-update-row"><span className="store-pill store-pill-info">◇ Info</span><span>Tomorrow&apos;s arrival window appears when the plan is published.</span></div>{home.openIssues > 0 && <Link to="/store/issues" className="store-update-row text-brand">{home.openIssues} open issue{home.openIssues === 1 ? '' : 's'} · View issues →</Link>}</section>
      </div>
      <section className="store-delivery-card"><h2>Today&apos;s delivery · {formatStoreDate(home.now.slice(0,10))}</h2>{delivery ? <><div className="store-delivery-status">✓ <span>{delivery.status === 'DELIVERED' ? 'Delivered' : delivery.status === 'PARTIAL' ? 'Partially delivered' : 'View delivery'}<small>{delivery.units} cases · {delivery.temp === 'CHILLED' ? 'Chilled' : 'Dry goods'}</small></span></div><Link to="/store/deliveries" className="store-outline-action">View proof and confirm receipt</Link></> : <p className="store-empty">No delivery scheduled today.</p>}</section>
    </div>
  </>
}

export function StoreOrders() {
  const query = useQuery({ queryKey: ['store', 'orders'], queryFn: () => storeApi.orders() })
  const [filter, setFilter] = useState<'upcoming' | 'needs' | 'past'>('upcoming')
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  const all = query.data.items
  const upcoming = all.filter(order => !['DELIVERED','PARTIAL','FAILED','CANCELLED'].includes(order.status))
  const needs = upcoming.filter(order => order.status === 'PREPARED' || !order.storeChecked)
  const orders = filter === 'upcoming' ? upcoming : filter === 'needs' ? needs : all.filter(order => !upcoming.includes(order))
  return <><Heading title="Orders" subtitle="Upcoming and past orders" />
    <div className="store-orders-toolbar"><div className="store-tabs" role="tablist" aria-label="Order filters"><button role="tab" aria-selected={filter === 'upcoming'} onClick={() => setFilter('upcoming')}>Upcoming {upcoming.length}</button><button role="tab" aria-selected={filter === 'needs'} onClick={() => setFilter('needs')}>⚠ Needs you {needs.length}</button><button role="tab" aria-selected={filter === 'past'} onClick={() => setFilter('past')}>Past</button></div><Link to="/store/orders/new" className="store-new-order">＋ New order</Link></div>
    <div className="store-orders-table"><div className="store-orders-head"><span>Delivery</span><span>Line</span><span>Cases</span><span>Status</span><span>Source</span><span>Order</span></div>{orders.length ? orders.map(order => <Link className="store-orders-entry" to={`/store/orders/${order.id}`} key={order.id}><strong>{formatStoreDate(order.runDate)}</strong><span>Fresh {order.temp === 'CHILLED' ? 'chilled' : 'dry'}</span><span>{order.units}</span><span><Status status={order.status} /></span><span>{order.source === 'SEED' ? 'Prepared' : order.source.replaceAll('_',' ').toLowerCase()}</span><span>{order.ref}</span></Link>) : <p className="store-empty">No orders in this section.</p>}</div>
    <p className="store-table-note">ⓘ Prepared orders come from your order history. Open an order to review it before cut-off.</p>
  </>
}

function formatStoreDate(value: string) { return new Date(`${value.slice(0,10)}T12:00:00`).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' }) }

export function StoreOrderDetail() {
  const { id = '' } = useParams()
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['store', 'order', id], queryFn: () => storeApi.order(id) })
  const pastOrders = useQuery({ queryKey: ['store', 'orders'], queryFn: () => storeApi.orders() })
  const home = useQuery({ queryKey: ['store', 'home'], queryFn: storeApi.home })
  const [units, setUnits] = useState<number | null>(null)
  const [dispute, setDispute] = useState('')
  const [success, setSuccess] = useState('')
  const mutation = useMutation({ mutationFn: async (action: 'edit' | 'confirm' | 'cancel' | 'check') => {
    if (action === 'edit') return storeApi.edit(id, units ?? query.data!.order.units)
    if (action === 'confirm') return storeApi.confirm(id)
    if (action === 'cancel') return storeApi.cancel(id)
    return storeApi.check(id, !dispute.trim(), dispute.trim() || undefined)
  }, onSuccess: async () => { setSuccess('Order updated.'); await client.invalidateQueries({ queryKey: ['store'] }) } })
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  const { order, history } = query.data
  const currentUnits = units ?? order.units
  const comparable = pastOrders.data?.items.filter(other => other.id !== id && other.temp === order.temp && other.runDate < order.runDate && other.status !== 'CANCELLED') ?? []
  const usual = comparable.length ? comparable.reduce((total, other) => total + other.units, 0) / comparable.length : null
  const unusual = usual !== null && currentUnits > usual * 3
  const closed = home.data?.ordersClosed && order.runDate === home.data.runDate
  return <><Link to="/store/orders" className="mb-4 inline-flex min-h-12 items-center font-semibold text-brand">← Orders</Link><Heading title={order.ref} subtitle={`${order.runDate} · ${order.brand}`} action={<Status status={order.status} />} />
    <Feedback error={mutation.error} success={success} />
    {order.status === 'MOVED' && <Card className="mb-4 border-pink-300"><h2 className="font-bold">↪ Order moved</h2><p className="mt-2 text-muted">This order is now on the {order.runDate} run. Check the history below for the reason.</p></Card>}
    {closed && order.status === 'PREPARED' && <Card className="mb-4 border-warning"><h2 className="font-bold">◷ Orders closed</h2><p className="mt-2 text-muted">Changes to this run are closed. Place a new order for the next run.</p></Card>}
    {order.status === 'CONFIRMED' && !order.storeChecked && <Card className="mb-4 border-warning"><h2 className="font-bold">Phone order needs your check</h2><p className="mt-2 text-muted">Dispatch entered this after your call. Check the details, then accept or tell them what is wrong.</p><textarea value={dispute} onChange={e => setDispute(e.target.value)} placeholder="What needs correcting? Leave blank if it is right." className="mt-4 min-h-24 w-full rounded-lg border border-line p-3" /><Button disabled={mutation.isPending} onClick={() => mutation.mutate('check')}>{dispute.trim() ? 'Send correction' : 'Looks right'}</Button></Card>}
    <Card><div className="grid gap-4 sm:grid-cols-3"><div><p className="text-sm text-muted">Cases</p><p className="text-2xl font-bold">{order.units}</p></div><div><p className="text-sm text-muted">Temperature</p><p className="text-lg font-semibold">{order.temp.toLowerCase()}</p></div><div><p className="text-sm text-muted">Source</p><p className="text-lg font-semibold">{order.source.replaceAll('_',' ').toLowerCase()}</p></div></div>
      {order.status === 'PREPARED' && !closed && <div className="mt-6 border-t border-line pt-5"><label className="block font-semibold">Edit cases <input type="number" min="1" value={currentUnits} onChange={e => setUnits(Number(e.target.value))} className="mt-2 block min-h-12 w-32 rounded-lg border border-line px-3" /></label>{unusual && <p role="alert" className="mt-2 text-warning">! More than 3× the usual quantity. Check before confirming.</p>}<div className="mt-4 flex flex-wrap gap-3"><Button disabled={mutation.isPending || currentUnits < 1} tone="secondary" onClick={() => mutation.mutate('edit')}>Save quantity</Button><Button disabled={mutation.isPending || unusual} onClick={() => mutation.mutate('confirm')}>Confirm order</Button><Button disabled={mutation.isPending} tone="danger" onClick={() => { if (window.confirm('Cancel this order?')) mutation.mutate('cancel') }}>Cancel order</Button></div></div>}
    </Card>
    <h2 className="mb-3 mt-8 text-xl font-bold">Order history</h2><Card>{history.length ? <ol className="space-y-4">{history.map((event, i) => <li key={i} className="border-l-2 border-brand pl-4"><p className="font-semibold">{event.type.replaceAll('_',' ').toLowerCase()}</p><p className="text-sm text-muted">{new Date(event.at).toLocaleString()} · {event.actor ?? 'System'}</p>{Boolean(event.details.reason) && <p className="text-sm">Reason: {String(event.details.reason)}</p>}</li>)}</ol> : <p className="text-muted">No changes recorded yet.</p>}</Card>
  </>
}

export function NewStoreOrder() {
  const navigate = useNavigate()
  const client = useQueryClient()
  const [runDate, setRunDate] = useState('')
  const [temp, setTemp] = useState<'CHILLED' | 'AMBIENT'>('AMBIENT')
  const [units, setUnits] = useState(1)
  const [note, setNote] = useState('')
  const mutation = useMutation({ mutationFn: () => storeApi.create({ runDate, temp, units, note }), onSuccess: async (order: Order) => { await client.invalidateQueries({ queryKey: ['store'] }); navigate(`/store/orders/${order.id}`) } })
  return <><Link to="/store/orders" className="mb-4 inline-flex min-h-12 items-center font-semibold text-brand">← Orders</Link><Heading title="New order" subtitle="Extra cases for a run" /><Feedback error={mutation.error} /><Card><form onSubmit={e => { e.preventDefault(); mutation.mutate() }} className="grid max-w-lg gap-5"><label className="font-semibold">Run date<input required type="date" value={runDate} onChange={e => setRunDate(e.target.value)} className="mt-2 block min-h-12 w-full rounded-lg border border-line px-3" /></label><label className="font-semibold">Cases<input required type="number" min="1" value={units} onChange={e => setUnits(Number(e.target.value))} className="mt-2 block min-h-12 w-full rounded-lg border border-line px-3" /></label><label className="font-semibold">Temperature<select value={temp} onChange={e => setTemp(e.target.value as 'CHILLED' | 'AMBIENT')} className="mt-2 block min-h-12 w-full rounded-lg border border-line px-3"><option value="AMBIENT">Ambient</option><option value="CHILLED">Chilled</option></select></label><label className="font-semibold">Note<textarea value={note} onChange={e => setNote(e.target.value)} maxLength={500} className="mt-2 block min-h-24 w-full rounded-lg border border-line p-3" /></label><Button type="submit" disabled={mutation.isPending || units < 1 || !runDate}>{mutation.isPending ? 'Saving…' : 'Place order'}</Button></form></Card></>
}
