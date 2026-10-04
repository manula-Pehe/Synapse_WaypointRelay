import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { storeApi, type Delivery, type Order } from './api'
import { storeLocalDate, storeTimeLabel } from './storeLive'
import { Loading } from './StoreShared'
import './store-history.css'

type Row = { order: Order; delivery?: Delivery }
const quote = (value: string | number) => `"${String(value).replaceAll('"', '""')}"`
function dateLabel(value: string) { return new Date(`${value}T12:00:00`).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' }) }

export function StoreHistory() {
  const [range, setRange] = useState<'today' | 'week' | 'month'>('week')
  const query = useQuery({ queryKey: ['store', 'history'], queryFn: async () => {
    const home = await storeApi.home()
    const today = new Date(home.now)
    const from = new Date(today.getTime() - 30 * 86_400_000)
    const orders = (await storeApi.orders(storeLocalDate(from), storeLocalDate(today))).items.filter(order => ['DELIVERED','PARTIAL','FAILED'].includes(order.status))
    const dates = [...new Set(orders.map(order => order.runDate))]
    const results = await Promise.allSettled(dates.map(date => storeApi.deliveries(date)))
    const deliveries = new Map<string, Delivery>(results.flatMap(result => result.status === 'fulfilled' ? result.value.items : []).map(delivery => [delivery.orderId, delivery]))
    return { today, rows: orders.map(order => ({ order, delivery: deliveries.get(order.id) })) as Row[] }
  }, refetchInterval: 30_000, refetchOnWindowFocus: 'always' })
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  const days = range === 'today' ? 0 : range === 'week' ? 7 : 30
  const from = new Date(query.data.today.getTime() - days * 86_400_000)
  const rows = query.data.rows.filter(row => row.order.runDate >= storeLocalDate(from)).sort((a,b) => b.order.runDate.localeCompare(a.order.runDate))
  const delivered = rows.filter(row => row.order.status !== 'FAILED')
  const tracked = delivered.filter(row => row.delivery?.arrival && row.delivery.delivery?.at)
  const onTime = tracked.filter(row => Date.parse(row.delivery!.delivery!.at!) <= Date.parse(row.delivery!.arrival!.to)).length
  const received = delivered.reduce((sum,row) => sum + (row.delivery?.receipt?.receivedUnits ?? 0), 0)
  const ordered = delivered.reduce((sum,row) => sum + row.order.units, 0)
  function exportCsv() {
    const csv = [['Date','Order','Line','Ordered cases','Delivered cases','Arrived','Receipt cases','Status'], ...rows.map(({ order, delivery }) => [order.runDate, order.ref, order.temp === 'CHILLED' ? 'Chilled' : 'Dry', order.units, delivery?.delivery?.units ?? '', delivery?.delivery?.at ?? '', delivery?.receipt?.receivedUnits ?? '', order.status])].map(values => values.map(value => quote(value)).join(',')).join('\r\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = `store-deliveries-${storeLocalDate(query.data!.today)}.csv`; anchor.click(); window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
  }
  return <div className="store-history-page"><div className="history-toolbar"><div className="history-filters">{([['today','Today'],['week','This week'],['month','Last 30 days']] as const).map(([value,label]) => <button key={value} className={range === value ? 'selected' : ''} onClick={() => setRange(value)}>{label}</button>)}</div><button onClick={exportCsv} disabled={!rows.length}>▤ Export CSV</button></div><div className="history-table"><div className="history-head"><span>Date</span><span>Order</span><span>Line</span><span>Cases</span><span>Arrived</span><span>Proof</span><span>Receipt</span></div>{rows.length ? rows.map(({ order, delivery }) => <div key={order.id} className="history-row"><strong>{dateLabel(order.runDate)}</strong><Link to={`/store/orders/${order.id}`}>{order.ref}</Link><span>{order.temp === 'CHILLED' ? 'Chilled' : 'Dry'}</span><span>{delivery?.delivery?.units ?? '—'} / {order.units}</span><span>{delivery?.delivery?.at ? storeTimeLabel(new Date(delivery.delivery.at)) : '—'}</span><span>{delivery?.delivery?.photoUrl || delivery?.delivery?.signatureUrl ? <Link to={`/store/deliveries/${order.id}`}>View</Link> : 'Unavailable'}</span><span>{delivery?.receipt ? `✓ Confirmed · ${delivery.receipt.receivedUnits}` : 'Not confirmed'}</span></div>) : <p className="store-empty">No past deliveries in this period.</p>}</div><div className="history-summary"><div><small>{range === 'today' ? 'Today' : range === 'week' ? 'This week' : 'Last 30 days'}</small><strong>{tracked.length ? `${onTime} of ${tracked.length} on time` : 'Timing unavailable'}</strong></div><div><small>Cases received</small><strong>{received} of {ordered}</strong></div><div><small>Receipts</small><strong>{delivered.filter(row => !!row.delivery?.receipt).length} confirmed</strong></div></div></div>
}
