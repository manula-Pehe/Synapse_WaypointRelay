import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { storeApi, type Order } from './api'
import { Button, Card, Feedback, Heading, Loading, OrderCard, Status } from './StoreShared'

export function StoreHome() {
  const query = useQuery({ queryKey: ['store', 'home'], queryFn: storeApi.home, refetchInterval: 30_000 })
  const [offline, setOffline] = useState(!navigator.onLine)
  useEffect(() => { const update = () => setOffline(!navigator.onLine); window.addEventListener('online', update); window.addEventListener('offline', update); return () => { window.removeEventListener('online', update); window.removeEventListener('offline', update) } }, [])
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  const home = query.data
  const ms = new Date(home.cutOffAt).getTime() - new Date(home.now).getTime()
  const until = ms <= 0 ? 'Cut-off passed' : `${Math.floor(ms / 3_600_000)}h ${Math.floor(ms % 3_600_000 / 60_000)}m until cut-off`
  return <>
    <Heading title="Store home" subtitle={`${home.outlet} · ${home.brand}`} action={<Link to="/store/orders/new" className="flex min-h-12 items-center rounded-lg bg-brand px-5 font-semibold text-on-brand">+ New order</Link>} />
    {offline && <p role="status" className="mb-5 rounded-xl bg-warning-soft p-4 font-medium text-warning">◇ Offline. Reconnect before saving changes or confirming an order.</p>}
    <div className="mb-6 grid gap-4 sm:grid-cols-3">
      <Card><p className="text-sm text-muted">Next run</p><p className="mt-1 text-xl font-bold">{home.runDate}</p><p className="mt-2 text-sm text-muted">{home.tomorrow.length} order{home.tomorrow.length === 1 ? '' : 's'}</p></Card>
      <Card><p className="text-sm text-muted">Order cut-off</p><p className="mt-1 text-xl font-bold">{home.ordersClosed ? '✓ Orders closed' : until}</p><p className="mt-2 text-sm text-muted">{new Date(home.cutOffAt).toLocaleString()}</p></Card>
      <Card><p className="text-sm text-muted">Open issues</p><p className="mt-1 text-xl font-bold">{home.openIssues}</p><Link to="/store/issues" className="mt-2 inline-flex min-h-12 items-center font-semibold text-brand">View issues →</Link></Card>
    </div>
    <Heading title="Tomorrow’s orders" action={<Link to="/store/orders" className="flex min-h-12 items-center font-semibold text-brand">All orders →</Link>} />
    <div className="grid gap-3 sm:grid-cols-2">{home.tomorrow.length ? home.tomorrow.map(order => <OrderCard key={order.id} order={order} />) : <Card>No order for this run yet.</Card>}</div>
    <Heading title="Today’s delivery" action={<Link to="/store/deliveries" className="flex min-h-12 items-center font-semibold text-brand">Delivery details →</Link>} />
    <div className="grid gap-3 sm:grid-cols-2">{home.today.length ? home.today.map(order => <OrderCard key={order.id} order={order} />) : <Card>No delivery scheduled today.</Card>}</div>
  </>
}

export function StoreOrders() {
  const query = useQuery({ queryKey: ['store', 'orders'], queryFn: () => storeApi.orders() })
  const [filter, setFilter] = useState('all')
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  const orders = query.data.items.filter(o => filter === 'all' || o.status === filter)
  return <><Heading title="Orders" subtitle="Upcoming and recent orders" action={<Link to="/store/orders/new" className="flex min-h-12 items-center rounded-lg bg-brand px-5 font-semibold text-on-brand">+ New order</Link>} />
    <label className="mb-5 block text-sm font-semibold">Status <select value={filter} onChange={e => setFilter(e.target.value)} className="ml-3 min-h-12 rounded-lg border border-line bg-surface px-3"><option value="all">All</option>{['PREPARED','CONFIRMED','PLANNED','ON_THE_WAY','DELIVERED','PARTIAL','FAILED','MOVED','CANCELLED'].map(s => <option key={s}>{s}</option>)}</select></label>
    <div className="grid gap-3 sm:grid-cols-2">{orders.length ? orders.map(o => <OrderCard key={o.id} order={o} />) : <Card>No orders match this filter.</Card>}</div>
  </>
}

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
