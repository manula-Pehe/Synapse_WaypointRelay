import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { storeApi, type Delivery } from './api'
import { Button, Card, Feedback, Heading, Loading, Status } from './StoreShared'

function DeliveryCard({ delivery }: { delivery: Delivery }) {
  const client = useQueryClient()
  const detail = useQuery({ queryKey: ['store', 'order', delivery.orderId], queryFn: () => storeApi.order(delivery.orderId), refetchInterval: 15_000, refetchOnWindowFocus: 'always', refetchOnReconnect: 'always' })
  const [units, setUnits] = useState<number | null>(null)
  const [note, setNote] = useState('')
  const [saved, setSaved] = useState(false)
  const receipt = useMutation({ mutationFn: () => storeApi.receipt(delivery.orderId, units ?? detail.data?.order.units ?? 0, note), onSuccess: async () => { setSaved(true); await client.invalidateQueries({ queryKey: ['store', 'deliveries'] }) } })
  const order = detail.data?.order
  return <Card><div className="flex flex-wrap justify-between gap-3"><div><h2 className="text-xl font-bold">{delivery.orderRef}</h2><p className="text-sm text-muted">{order?.units ?? '…'} cases</p></div><Status status={delivery.status} /></div>
    {delivery.arrival ? <div className="mt-5 rounded-xl bg-brand-soft p-4"><p className="font-semibold">Arrival: {new Date(delivery.arrival.from).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}–{new Date(delivery.arrival.to).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</p>{delivery.arrival.changedReason && <p className="mt-2 text-sm">Changed: {delivery.arrival.changedReason}</p>}{delivery.arrival.lateRisk && delivery.arrival.lateRisk > .5 && <p className="mt-2 text-warning">! Arrival may be late</p>}</div> : <p className="mt-5 text-muted">{delivery.status === 'CONFIRMED' ? 'Waiting for tonight’s plan. Arrival time will appear when published.' : 'Arrival time has not been published yet.'}</p>}
    {delivery.driverStatus?.offline && <p className="mt-4 rounded-lg bg-inset p-3 text-muted">◇ Driver offline. Last known update: {delivery.driverStatus.lastSyncAt ? new Date(delivery.driverStatus.lastSyncAt).toLocaleString() : 'unknown'}.</p>}
    {delivery.deferral && <p className="mt-4 rounded-lg bg-pink-100 p-3 text-pink-800">↪ Moved to {delivery.deferral.newDate}: {delivery.deferral.reason}</p>}
    {(delivery.deferral || delivery.status === 'MOVED') && <Link to={`/store/deliveries/${delivery.orderId}/moved`} className="mt-2 inline-flex min-h-12 items-center font-semibold text-brand">Review moved order →</Link>}
    {delivery.shortfall && <p className="mt-4 rounded-lg bg-warning-soft p-3 text-warning">! Shortfall: {delivery.shortfall.missingUnits} cases. {delivery.shortfall.reason}</p>}
    {(delivery.shortfall || delivery.breakdown || delivery.status === 'FAILED') && <Link to={`/store/deliveries/${delivery.orderId}/problem`} className="mt-2 inline-flex min-h-12 items-center font-semibold text-brand">Review delivery problem →</Link>}
    {delivery.delivery && <Card className="mt-5 bg-canvas"><h3 className="font-bold">Delivery proof</h3><p>{delivery.delivery.units} cases · {delivery.delivery.outcome.toLowerCase()}</p>{delivery.delivery.receivedBy && <p>Received by {delivery.delivery.receivedBy}</p>}{delivery.delivery.photoUrl && <a href={delivery.delivery.photoUrl} target="_blank" rel="noreferrer" className="mr-4 text-brand underline">View photo</a>}{delivery.delivery.signatureUrl && <a href={delivery.delivery.signatureUrl} target="_blank" rel="noreferrer" className="text-brand underline">View signature</a>}</Card>}
    {delivery.receipt ? <p className="mt-5 font-semibold text-success">✓ Receipt confirmed: {delivery.receipt.receivedUnits} cases</p> : ['DELIVERED','PARTIAL'].includes(delivery.status) && <div className="mt-5 border-t border-line pt-5"><h3 className="font-bold">Confirm what arrived</h3><p className="mt-1 text-sm text-muted">Compare with the driver’s proof before confirming.</p><div className="mt-4 flex flex-wrap gap-3"><label className="font-semibold">Cases received<input type="number" min="0" max={order?.units} value={units ?? order?.units ?? 0} onChange={e => setUnits(Number(e.target.value))} className="mt-2 block min-h-12 w-32 rounded-lg border border-line px-3" /></label><label className="grow font-semibold">Note<input value={note} onChange={e => setNote(e.target.value)} className="mt-2 block min-h-12 w-full rounded-lg border border-line px-3" /></label></div><div className="mt-4"><Button disabled={receipt.isPending || !order || (units ?? order.units) > order.units} onClick={() => receipt.mutate()}>Confirm receipt</Button></div><Feedback error={receipt.error} success={saved ? 'Receipt recorded.' : undefined} /></div>}
    <Link to={`/store/deliveries/${delivery.orderId}`} className="mt-5 inline-flex min-h-12 items-center font-semibold text-brand">Arrival and order history →</Link>
  </Card>
}
export function StoreDeliveries() {
  const query = useQuery({ queryKey: ['store', 'deliveries'], queryFn: () => storeApi.deliveries(), refetchInterval: 15_000, refetchOnWindowFocus: 'always', refetchOnReconnect: 'always' })
  return <><Heading title="Deliveries" subtitle="Today’s arrivals and receipts" />{!query.data ? <Loading error={query.error} retry={() => void query.refetch()} /> : <div className="space-y-4">{query.data.items.length ? query.data.items.map(d => <DeliveryCard key={d.orderId} delivery={d} />) : <Card>No delivery for today.</Card>}</div>}</>
}
