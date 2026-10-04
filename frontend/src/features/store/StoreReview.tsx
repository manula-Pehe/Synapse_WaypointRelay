import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { storeApi, type Order } from './api'
import { storeCutoffLabel, storeTimeLabel, storeWindowLabel, useStoreLiveNow } from './storeLive'
import { Feedback, Loading } from './StoreShared'
import './store-review.css'

type Dialog = { kind: 'unusual' | 'cancel'; order: Order; units: number } | null

function label(order: Order) { return order.temp === 'CHILLED' ? 'Chilled' : 'Dry goods' }
function date(value: string) { return new Date(`${value}T12:00:00`).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' }) }

export function StoreReview() {
  const client = useQueryClient()
  const home = useQuery({ queryKey: ['store', 'home'], queryFn: storeApi.home, refetchInterval: 15_000 })
  const list = useQuery({ queryKey: ['store', 'orders'], queryFn: () => storeApi.orders(), refetchInterval: 15_000 })
  const outlet = useQuery({ queryKey: ['store', 'outlet', home.data?.outlet], queryFn: () => storeApi.outlet(home.data!.outlet), enabled: !!home.data?.outlet })
  const now = useStoreLiveNow(home.data?.now, home.dataUpdatedAt)
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [dialog, setDialog] = useState<Dialog>(null)
  const [accepted, setAccepted] = useState<Record<string, number>>({})
  const [reason, setReason] = useState('Enough stock')
  const [message, setMessage] = useState('')
  const [actionError, setActionError] = useState<unknown>()
  const orders = home.data?.tomorrow.filter(order => order.status !== 'CANCELLED') ?? []
  const closed = !!home.data && (home.data.ordersClosed || (!!now && now.getTime() >= Date.parse(home.data.cutOffAt)))
  const editable = !closed

  const mutation = useMutation({
    mutationFn: async ({ action, order, units }: { action: 'confirm' | 'edit' | 'cancel'; order: Order; units?: number }) => {
      if (action === 'cancel') return storeApi.cancel(order.id, reason)
      const next = units ?? order.units
      if (next !== order.units) await storeApi.edit(order.id, next)
      if (action === 'confirm' && order.status === 'PREPARED') return storeApi.confirm(order.id)
      return storeApi.order(order.id)
    },
    onSuccess: async (_, variables) => {
      setDialog(null)
      setActionError(undefined)
      setMessage(variables.action === 'cancel' ? `${label(variables.order)} order cancelled.` : variables.action === 'edit' ? 'Quantity updated.' : 'Order confirmed.')
      await client.invalidateQueries({ queryKey: ['store'] })
    },
    onError: error => { setDialog(null); setActionError(error); void client.invalidateQueries({ queryKey: ['store'] }) },
  })

  if (!home.data || !list.data) return <Loading error={home.error ?? list.error} retry={() => { void home.refetch(); void list.refetch() }} />
  const runDate = home.data.runDate
  const unusual = (order: Order, units: number) => {
    const previous = list.data.items.filter(item => item.id !== order.id && item.temp === order.temp && item.runDate < order.runDate && item.status !== 'CANCELLED')
    const usual = previous.length ? previous.reduce((sum, item) => sum + item.units, 0) / previous.length : null
    return usual !== null && units > usual * 3 ? Math.round(usual) : null
  }
  const submit = async (order: Order, action: 'confirm' | 'edit') => {
    const units = quantities[order.id] ?? order.units
    if (!Number.isInteger(units) || units < 1 || mutation.isPending) return
    const average = unusual(order, units)
    if (average !== null && accepted[order.id] !== units) { setDialog({ kind: 'unusual', order, units }); return }
    setActionError(undefined)
    mutation.mutate({ action, order, units })
  }
  const allConfirmed = orders.length > 0 && orders.every(order => order.status === 'CONFIRMED')
  const total = orders.reduce((sum, order) => sum + (quantities[order.id] ?? order.units), 0)
  const weight = orders.reduce((sum, order) => sum + order.weightKg * (quantities[order.id] ?? order.units) / Math.max(1, order.units), 0)
  const windowText = outlet.data ? storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose) : null
  return <div className="store-review">
    <div className="review-mobile-cutoff">◷ {now ? storeCutoffLabel(now, home.data.cutOffAt, closed) : 'Loading cut-off…'}</div>
    {closed && <div className="review-closed" role="status">▦ Orders for {date(runDate)} closed at {storeTimeLabel(new Date(home.data.cutOffAt))}. Changes now go on the next run.</div>}
    {closed && orders.some(order => order.status === 'PREPARED' && order.temp === 'CHILLED') && <div className="review-missed" role="alert"><strong>⊗ Your chilled order wasn’t confirmed - it is not on this run.</strong><span>Call dispatch before the plan is published, or order for the next run.</span></div>}
    {allConfirmed && <div className="review-success" role="status"><strong>⊙ {orders.length === 1 ? 'Order is confirmed' : 'All orders are confirmed'}</strong><span>They go into tonight’s plan. You’ll get the arrival window after the plan is published.</span></div>}
    <Feedback error={actionError} success={message} />
    <div className="review-columns"><div className="review-order-list">
      {orders.length ? orders.map(order => {
        const units = quantities[order.id] ?? order.units
        const average = unusual(order, units)
        const canChange = editable && (order.status === 'PREPARED' || order.status === 'CONFIRMED')
        return <section className="review-order-card" key={order.id}>
          <div className="review-order-top"><img src={`/store-icons/${order.temp === 'CHILLED' ? 'snow' : 'box'}.svg`} alt="" width="24" height="24" /><div className="review-order-name"><h2>{label(order)} <span className="review-desktop">(cases)</span></h2><p>{order.temp === 'CHILLED' ? 'Chilled order' : 'Prepared order'} · {order.ref}</p></div>{order.status === 'CONFIRMED' && <span className="store-pill store-pill-success">⊙ Confirmed{order.autoConfirm ? ' · auto' : ''}</span>}</div>
          {canChange ? <div className="review-quantity"><button type="button" aria-label={`Remove one ${label(order)} case`} disabled={units <= 1 || mutation.isPending} onClick={() => setQuantities({ ...quantities, [order.id]: units - 1 })}>−</button><input aria-label={`${label(order)} cases`} type="number" min="1" step="1" value={units} onChange={event => setQuantities({ ...quantities, [order.id]: Number(event.target.value) })} /><button type="button" aria-label={`Add one ${label(order)} case`} disabled={mutation.isPending} onClick={() => setQuantities({ ...quantities, [order.id]: units + 1 })}>＋</button></div> : <strong className="review-readonly-count">{order.units} cases</strong>}
          <div className="review-order-meta"><span>Estimated ≈ {Math.round(order.weightKg * units / Math.max(1, order.units))} kg · {(order.volumeM3 * units / Math.max(1, order.units)).toFixed(1)} m³</span>{order.status === 'PREPARED' && <span>Auto-confirm <strong>{order.autoConfirm ? 'On' : 'Off'}</strong></span>}</div>
          {average !== null && <p className="review-inline-warning" role="alert">⚠ {units} cases is more than 3× your usual {average}. You’ll check this before confirming.</p>}
          {order.status === 'PREPARED' && <p className="review-info">ⓘ {order.autoConfirm ? 'Auto-confirm is on at cut-off if unchanged.' : 'This order needs your confirmation before cut-off.'}</p>}
          {canChange && <div className="review-card-actions">{order.status === 'CONFIRMED' && <button type="button" onClick={() => void submit(order, 'edit')} disabled={mutation.isPending || units === order.units || units < 1}>✎ Edit quantity</button>}<button type="button" className="review-cancel-link" onClick={() => { setReason('Enough stock'); setDialog({ kind: 'cancel', order, units }) }}>× Cancel order</button></div>}
        </section>
      }) : <div className="review-order-card">No orders are prepared for this run. <Link to="/store/orders/new">Place a new order</Link>.</div>}
    </div><div className="review-side"><section className="review-summary"><h2>Summary</h2><p>{orders.length} orders · {total} cases · ≈ {Math.round(weight)} kg</p><p>Delivery: {date(runDate)}{windowText ? `, window ${windowText}` : ''}. You will see a predicted arrival time after the plan is published.</p>{!allConfirmed && editable && orders.some(order => order.status === 'PREPARED') && <button type="button" className="review-confirm" disabled={mutation.isPending || orders.some(order => (quantities[order.id] ?? order.units) < 1)} onClick={async () => { for (const order of orders) { if (order.status !== 'PREPARED') continue; const units = quantities[order.id] ?? order.units; const average = unusual(order, units); if (average !== null && accepted[order.id] !== units) { setDialog({ kind: 'unusual', order, units }); return } try { if (units !== order.units) await storeApi.edit(order.id, units); await storeApi.confirm(order.id) } catch (error) { setActionError(error); await client.invalidateQueries({ queryKey: ['store'] }); return } } setMessage('Orders confirmed.'); await client.invalidateQueries({ queryKey: ['store'] }) }}>⊙ {mutation.isPending ? 'Confirming…' : 'Confirm orders'}</button>}</section><section className="review-next"><h2>What happens next</h2><ol><li>The dispatcher plans all orders after cut-off.</li><li>You get an arrival window, or a clear notice if an order has to move.</li><li>The driver records proof; you confirm what arrived.</li></ol></section></div></div>
    {dialog && <div className="review-dialog-backdrop" role="presentation"><div className="review-dialog" role="dialog" aria-modal="true" aria-labelledby="review-dialog-title">{dialog.kind === 'unusual' ? <><h2 id="review-dialog-title">⚠ {dialog.units} {label(dialog.order).toLowerCase()} cases is about {Math.round(dialog.units / Math.max(1, unusual(dialog.order, dialog.units) ?? 1))}× your usual</h2><p>You usually order {unusual(dialog.order, dialog.units)} cases. Please check this quantity before confirming.</p><div className="review-dialog-comparison"><span>Usual<strong>{unusual(dialog.order, dialog.units)} cases</strong></span><span>This order<strong>{dialog.units} cases</strong></span></div><div className="review-dialog-actions"><button className="review-confirm" onClick={() => { setAccepted({ ...accepted, [dialog.order.id]: dialog.units }); setDialog(null); setMessage('Quantity checked. Confirm your orders to continue.') }}>⊙ Yes, this is right</button><button onClick={() => setDialog(null)}>Change quantity</button></div></> : <><h2 id="review-dialog-title">Cancel this {label(dialog.order).toLowerCase()} order?</h2><p>Your {dialog.order.units} cases for {date(dialog.order.runDate)} will be cancelled. This affects only this day’s order.</p><label>Reason (helps dispatch)<select value={reason} onChange={event => setReason(event.target.value)}><option>Enough stock</option><option>Store closed</option><option>Other</option></select></label><p className="review-info">ⓘ You can make a new order until cut-off.</p><div className="review-dialog-actions"><button className="review-danger" disabled={mutation.isPending} onClick={() => mutation.mutate({ action: 'cancel', order: dialog.order })}>Cancel order</button><button onClick={() => setDialog(null)}>Keep the order</button></div></>}</div></div>}
  </div>
}
