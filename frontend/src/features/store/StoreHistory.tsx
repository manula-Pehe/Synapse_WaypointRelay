import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { storeApi, type Delivery } from './api'
import { Card, Heading, Loading, Status } from './StoreShared'

export function StoreHistory() {
  const query = useQuery({ queryKey: ['store', 'history'], queryFn: async () => {
    const orders = (await storeApi.orders()).items.filter(order => ['DELIVERED','PARTIAL','FAILED','CANCELLED'].includes(order.status))
    const dates = [...new Set(orders.map(order => order.runDate))]
    const byDate = await Promise.all(dates.map(date => storeApi.deliveries(date)))
    const deliveries = new Map<string, Delivery>(byDate.flatMap(group => group.items).map(delivery => [delivery.orderId, delivery]))
    return orders.map(order => ({ order, delivery: deliveries.get(order.id) }))
  } })
  return <><Heading title="Delivery history" subtitle="Past runs and receipt records" />{!query.data ? <Loading error={query.error} retry={() => void query.refetch()} /> : <div className="space-y-3">{query.data.length ? query.data.map(({ order, delivery }) => <Card key={order.id}><div className="flex flex-wrap justify-between gap-3"><div><Link to={`/store/orders/${order.id}`} className="font-bold text-brand">{order.ref} →</Link><p className="text-sm text-muted">{order.runDate} · {order.units} cases</p></div><Status status={order.status} /></div>{delivery?.receipt && <p className="mt-4 font-medium text-success">✓ Received {delivery.receipt.receivedUnits} cases on {new Date(delivery.receipt.at).toLocaleString()}</p>}{delivery?.delivery?.photoUrl && <a className="mt-3 inline-flex min-h-12 items-center text-brand underline" href={delivery.delivery.photoUrl}>View delivery photo</a>}{delivery?.delivery?.signatureUrl && <a className="mt-3 ml-3 inline-flex min-h-12 items-center text-brand underline" href={delivery.delivery.signatureUrl}>View signature</a>}</Card>) : <Card>No past deliveries yet.</Card>}</div>}</>
}
