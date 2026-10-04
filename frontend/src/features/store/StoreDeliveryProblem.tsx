import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { storeApi } from './api'
import { storeTimeLabel } from './storeLive'
import { Feedback, Loading, Status } from './StoreShared'
import './store-delivery-problem.css'

export function StoreDeliveryProblem() {
  const { orderId = '' } = useParams()
  const client = useQueryClient()
  const order = useQuery({ queryKey: ['store', 'order', orderId], queryFn: () => storeApi.order(orderId) })
  const deliveries = useQuery({ queryKey: ['store', 'deliveries', order.data?.order.runDate], queryFn: () => storeApi.deliveries(order.data!.order.runDate), enabled: !!order.data?.order.runDate, refetchInterval: 15_000 })
  const choice = useMutation({ mutationFn: (value: 'REPLAN_TOMORROW' | 'TRY_LATER_TODAY' | 'CANCEL') => {
    if (!delivery?.delivery?.id) throw Error('Dispatch has not published this choice yet.')
    return storeApi.failedChoice(delivery.delivery.id, value)
  }, onSuccess: async () => client.invalidateQueries({ queryKey: ['store', 'deliveries'] }) })
  if (!order.data) return <Loading error={order.error} retry={() => void order.refetch()} />
  if (!deliveries.data) return <Loading error={deliveries.error} retry={() => void deliveries.refetch()} />
  const current = order.data.order
  const delivery = deliveries.data.items.find(item => item.orderId === orderId)
  const shortfall = delivery?.shortfall
  const breakdown = delivery?.breakdown
  const failed = current.status === 'FAILED'
  const problem = failed ? 'failed' : breakdown ? 'breakdown' : shortfall ? 'shortfall' : null
  return <div className="store-problem-page"><Link to="/store/deliveries" className="problem-back">← Deliveries</Link><header className="problem-heading"><div><h1>Order {current.ref} · {current.temp === 'CHILLED' ? 'Chilled' : 'Dry goods'} · {current.units} cases</h1><p>For delivery {current.runDate} · {current.outletName}</p></div><Status status={current.status} /></header><div className="problem-columns"><main>{problem ? <><section className={`problem-alert ${failed || breakdown ? 'critical' : ''}`}><strong>{failed ? 'ⓧ Delivery could not be completed' : breakdown ? 'ⓧ The truck has a problem' : `⚠ ${shortfall!.missingUnits} cases are missing before departure`}</strong><p>{failed ? 'The delivery was not completed. Choose how you would like dispatch to proceed.' : breakdown ? 'Dispatch will contact you about this delivery.' : `${shortfall!.reason}. ${Math.max(0, current.units - shortfall!.missingUnits)} of ${current.units} cases are expected on this truck.`}</p></section>{shortfall && <section className="problem-card"><h2>Today’s delivery</h2><dl><dt>Expected on truck</dt><dd>{Math.max(0, current.units - shortfall.missingUnits)} cases</dd><dt>Missing</dt><dd>{shortfall.missingUnits} cases</dd>{delivery?.arrival && <><dt>Predicted arrival</dt><dd>{storeTimeLabel(new Date(delivery.arrival.from))} – {storeTimeLabel(new Date(delivery.arrival.to))}</dd></>}</dl></section>}{failed && <section className="problem-card"><h2>What would you like to do?</h2>{delivery?.delivery?.id ? <div className="problem-choice-row"><button className="primary" disabled={choice.isPending} onClick={() => choice.mutate('REPLAN_TOMORROW')}>Deliver tomorrow</button><button disabled={choice.isPending} onClick={() => choice.mutate('TRY_LATER_TODAY')}>Try later today</button><button className="danger" disabled={choice.isPending} onClick={() => { if (window.confirm('Cancel this order?')) choice.mutate('CANCEL') }}>Cancel this order</button></div> : <p>Dispatch has not published delivery choices yet. Contact dispatch to arrange another attempt.</p>}</section>}<Feedback error={choice.error} success={choice.isSuccess ? 'Your choice was sent to dispatch.' : undefined} /></> : <section className="problem-card"><h2>No delivery problem has been reported</h2><p>Current status: {current.status.replaceAll('_',' ').toLowerCase()}.</p></section>}</main><aside className="problem-card"><h2>{shortfall ? 'Why you see this early' : failed ? 'Driver’s proof' : 'Delivery updates'}</h2>{shortfall ? <p>Shortfalls are shown as soon as dispatch publishes them, before the truck reaches your store.</p> : delivery?.delivery ? <><p>{delivery.delivery.receivedBy ? `Received by ${delivery.delivery.receivedBy}` : delivery.delivery.outcome}</p>{delivery.delivery.photoUrl && <a href={delivery.delivery.photoUrl} target="_blank" rel="noreferrer">View photo</a>}{delivery.delivery.signatureUrl && <a href={delivery.delivery.signatureUrl} target="_blank" rel="noreferrer">View signature</a>}</> : <p>Proof and detailed driver notes will appear when published.</p>}</aside></div></div>
}
