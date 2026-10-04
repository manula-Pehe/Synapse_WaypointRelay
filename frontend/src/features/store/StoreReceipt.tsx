import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { storeApi } from './api'
import { storeTimeLabel } from './storeLive'
import { Feedback, Loading, Status } from './StoreShared'
import './store-receipt.css'

function deliveredFromHistory(history: Awaited<ReturnType<typeof storeApi.order>>['history'], fallback: number) {
  const event = history.filter(item => item.type === 'DELIVERED' || item.type === 'PARTIAL').at(-1)
  const units = event?.details.units
  return typeof units === 'number' && Number.isInteger(units) && units >= 0 ? units : fallback
}

export function StoreReceipt() {
  const { orderId = '' } = useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const order = useQuery({ queryKey: ['store', 'order', orderId], queryFn: () => storeApi.order(orderId), refetchOnWindowFocus: 'always' })
  const deliveries = useQuery({ queryKey: ['store', 'deliveries', order.data?.order.runDate], queryFn: () => storeApi.deliveries(order.data!.order.runDate), enabled: !!order.data?.order.runDate, refetchOnWindowFocus: 'always' })
  const receipt = useMutation({ mutationFn: (receivedUnits: number) => storeApi.receipt(orderId, receivedUnits, ''), onSuccess: async () => {
    await Promise.all([client.invalidateQueries({ queryKey: ['store', 'deliveries'] }), client.invalidateQueries({ queryKey: ['store', 'order', orderId] })])
    navigate('/store/deliveries', { replace: true })
  } })
  if (!order.data) return <Loading error={order.error} retry={() => void order.refetch()} />
  if (!deliveries.data) return <Loading error={deliveries.error} retry={() => void deliveries.refetch()} />
  const current = order.data.order
  const delivery = deliveries.data.items.find(item => item.orderId === orderId)
  const proof = delivery?.delivery
  const deliveredUnits = deliveredFromHistory(order.data.history, proof?.units ?? current.units)
  const canConfirm = !!proof && ['DELIVERED', 'PARTIAL'].includes(current.status) && !delivery?.receipt
  const issueState = { orderId, units: Math.max(1, current.units - deliveredUnits), type: current.status === 'PARTIAL' ? 'MISSING' : 'DAMAGED' }
  return <div className="store-receipt-page"><Link to="/store/deliveries" className="receipt-back">← Deliveries</Link><header className="receipt-heading"><div><h1>Order {current.ref} · {current.temp === 'CHILLED' ? 'Chilled' : 'Dry goods'} · {current.units} cases</h1><p>{proof?.at ? `Delivered ${new Date(proof.at).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' })}` : `For ${current.runDate}`} · {current.outletName}</p></div><Status status={current.status} /></header>
    <div className="receipt-columns"><div className="receipt-left"><section className="receipt-proof"><h2>Driver’s proof of delivery</h2>{proof ? <><div className="receipt-proof-assets"><div className="receipt-proof-asset">{proof.photoUrl ? <a href={proof.photoUrl} target="_blank" rel="noreferrer">▣ View delivery photo</a> : <span>Photo unavailable</span>}</div><div className="receipt-proof-asset">{proof.signatureUrl ? <a href={proof.signatureUrl} target="_blank" rel="noreferrer">✎ View signature</a> : <span>{proof.receivedBy ? `Received by ${proof.receivedBy}` : 'Signature unavailable'}</span>}</div></div><dl><dt>Delivered</dt><dd>{deliveredUnits} of {current.units} cases</dd>{proof.at && <><dt>Arrived</dt><dd>{storeTimeLabel(new Date(proof.at))}</dd></>}{delivery?.arrival?.vehicleId && <><dt>Vehicle</dt><dd>{delivery.arrival.vehicleId}</dd></>}</dl>{delivery?.shortfall && <p className="receipt-shortfall">{delivery.shortfall.missingUnits} cases short · {delivery.shortfall.reason}</p>}</> : <p className="receipt-unavailable">The driver’s delivery proof has not been published. Receipt confirmation will be available when it arrives.</p>}</section>{delivery?.deferral?.newDate && <div className="receipt-reminder">↪ Remaining cases moved to {delivery.deferral.newDate}.</div>}</div>
      <section className="receipt-actions"><h2>Does this match what you received?</h2>{delivery?.receipt ? <p role="status" className="receipt-confirmed">✓ Confirmed {delivery.receipt.receivedUnits} cases</p> : <>{canConfirm && <button className="receipt-confirm" disabled={receipt.isPending} onClick={() => receipt.mutate(deliveredUnits)}>✓ {receipt.isPending ? 'Confirming…' : `Yes — confirm ${deliveredUnits} cases received`}</button>}{!proof && <p className="receipt-help">Waiting for driver proof. You can still report a problem to dispatch.</p>}<h3>Or report a problem</h3><div className="receipt-issue-options">{(['Short','Damaged','Wrong item','Temperature'] as const).map((label, index) => <Link key={label} to="/store/issues/new" state={{ ...issueState, type: ['MISSING','DAMAGED','WRONG_ITEM','OTHER'][index], note: label === 'Temperature' ? 'Temperature issue: ' : '' }}>{label}</Link>)}</div><Link className="receipt-report" to="/store/issues/new" state={issueState}>▣ Add photo and report</Link><p className="receipt-help">Your receipt stays unconfirmed until you confirm it. Report a mismatch to dispatch.</p></>}<Feedback error={receipt.error} /></section></div></div>
}
