import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { storeApi, type OrderStatus } from './api'
import { storeTimeLabel, storeWindowLabel } from './storeLive'
import { Loading, Status } from './StoreShared'
import './store-arrival.css'

const stages: { status: OrderStatus; label: string }[] = [
  { status: 'CONFIRMED', label: 'Confirmed' }, { status: 'PLANNED', label: 'Planned' },
  { status: 'LOADED', label: 'Loaded' }, { status: 'ON_THE_WAY', label: 'On the way' },
  { status: 'DELIVERED', label: 'Delivered' },
]
function range(from: string, to: string) { return `${storeTimeLabel(new Date(from))} – ${storeTimeLabel(new Date(to))}` }

export function StoreArrival() {
  const { orderId = '' } = useParams()
  const [acknowledged, setAcknowledged] = useState(false)
  const order = useQuery({ queryKey: ['store', 'order', orderId], queryFn: () => storeApi.order(orderId), refetchInterval: 15_000, refetchOnWindowFocus: 'always' })
  const outlet = useQuery({ queryKey: ['store', 'outlet', order.data?.order.outletId], queryFn: () => storeApi.outlet(order.data!.order.outletId), enabled: !!order.data?.order.outletId })
  const deliveries = useQuery({ queryKey: ['store', 'deliveries', order.data?.order.runDate], queryFn: () => storeApi.deliveries(order.data!.order.runDate), enabled: !!order.data?.order.runDate, refetchInterval: 15_000, refetchOnWindowFocus: 'always' })
  if (!order.data) return <Loading error={order.error} retry={() => void order.refetch()} />
  const current = order.data.order
  const delivery = deliveries.data?.items.find(item => item.orderId === orderId)
  const arrival = delivery?.arrival
  const offline = delivery?.driverStatus?.offline
  const late = !!arrival?.lateRisk && arrival.lateRisk >= .3
  const changed = !!arrival?.changedReason
  const stage = stages.findIndex(item => item.status === current.status)
  const windowText = outlet.data ? storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose) : null
  return <div className="store-arrival-page"><Link to="/store/deliveries" className="arrival-back">← Deliveries</Link><div className="arrival-heading"><div><h1>Order {current.ref} · {current.temp === 'CHILLED' ? 'Chilled' : 'Dry goods'} · {current.units} cases</h1><p>For delivery {new Date(`${current.runDate}T12:00:00`).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })} · {current.outletName}</p></div><Status status={current.status} /></div><div className="arrival-columns"><div className="arrival-main">
      <section className="arrival-progress" aria-label="Order progress">{stages.map((step, index) => <div key={step.status} className={index <= stage ? 'done' : ''}><span>{index < stage ? '✓' : index + 1}</span><small>{step.label}</small></div>)}</section>
      {current.status === 'CONFIRMED' && <section className="arrival-info"><strong>ⓘ Waiting for the plan</strong><p>Your order is confirmed. The predicted arrival window will appear when dispatch publishes the plan.</p></section>}
      {arrival ? <section className={`arrival-window${late || changed ? ' arrival-window-warning' : ''}${offline ? ' arrival-window-offline' : ''}`}><small>{offline ? 'DRIVER OFFLINE · LAST CONFIRMED WINDOW' : changed ? 'ARRIVAL TIME CHANGED' : 'PREDICTED ARRIVAL'}</small><h2>{range(arrival.from, arrival.to)}</h2>{windowText && <p>Inside your {windowText} delivery window.</p>}{arrival.changedReason && <p>{arrival.changedReason}</p>}{late && <p>⚠ There is a {Math.round(arrival.lateRisk! * 100)}% risk of arriving after the planned window.</p>}{offline && <p>Updates will resume when the driver’s phone reconnects. This is the last confirmed estimate.</p>}{(late || changed) && <button onClick={() => setAcknowledged(true)} disabled={acknowledged}>{acknowledged ? '✓ Staff plan noted' : `Plan staff for ${storeTimeLabel(new Date(arrival.from))}`}</button>}</section> : ['PLANNED','LOADED','ON_THE_WAY'].includes(current.status) && <section className="arrival-info"><strong>Arrival window not published yet</strong><p>Dispatch has updated the order status. Check back for a predicted time.</p></section>}
      {offline && delivery?.driverStatus?.lastSyncAt && <p className="arrival-last-sync">◌ Last driver update: {new Date(delivery.driverStatus.lastSyncAt).toLocaleString()}</p>}
      <section className="arrival-order-summary"><h2>Order</h2><dl><dt>Line</dt><dd>{current.temp === 'CHILLED' ? 'Fresh chilled' : 'Fresh dry'}</dd><dt>Quantity</dt><dd>{current.units} cases</dd>{windowText && <><dt>Your window</dt><dd>{windowText}</dd></>}</dl></section>
    </div><aside className="arrival-history"><h2>Order history</h2>{order.data.history.length ? <ol>{order.data.history.map((event, index) => <li key={`${event.at}-${index}`}><time>{new Date(event.at).toLocaleString('en-GB', { dateStyle: 'short', timeStyle: 'short' })}</time><span>{event.type.replaceAll('_', ' ').toLowerCase()}{typeof event.details.reason === 'string' ? ` · ${event.details.reason}` : ''}</span></li>)}</ol> : <p>No history recorded yet.</p>}</aside></div></div>
}
