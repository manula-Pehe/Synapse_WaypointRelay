import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { dispatchApi } from '../features/dispatch/core/api'
import { Icon } from '../ui/icons'

const card = 'rounded-xl border border-line bg-surface p-5'
const statusText: Record<string, string> = { PLANNED: 'Planned', ON_THE_WAY: 'On the way', COMPLETED: 'Completed' }

export default function LiveBoardPage({ runDate, depot }: { runDate: string; depot: string }) {
  const [selectedOrder, setSelectedOrder] = useState<string | null>(null)
  const board = useQuery({ queryKey: ['dispatch-live', runDate, depot], queryFn: () => dispatchApi.live(runDate, depot), refetchInterval: 15_000 })
  const detail = useQuery({ queryKey: ['dispatch-live-order', selectedOrder], queryFn: () => dispatchApi.order(selectedOrder!), enabled: !!selectedOrder })
  if (board.isPending) return <p role="status">Loading live board…</p>
  if (board.error) return <div role="alert" className="rounded-xl bg-danger-soft p-5 text-danger">Could not load live board: {board.error.message}</div>
  const { onTimePercent, onTimeStops, completedStops, deferredToday, skippedTwoRuns, fridgeTruckUsePercent, needsAttention, trips } = board.data
  return <div className="space-y-5 text-ink">
    <div className="flex flex-wrap items-center justify-between gap-2"><p className="text-sm text-muted">{depot} · {runDate} · updates every 15 seconds</p><p className="text-xs text-muted">Last response {new Date(board.data.generatedAt).toLocaleTimeString()}</p></div>
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      {[
        ['On time so far', onTimePercent == null ? '—' : `${onTimePercent}%`, `${onTimeStops} of ${completedStops} completed stops with outcomes`],
        ['Deferred today', deferredToday == null ? '—' : String(deferredToday), deferredToday == null ? 'No published plan' : 'Published plan'],
        ['Skipped 2+ runs', skippedTwoRuns == null ? '—' : String(skippedTwoRuns), skippedTwoRuns == null ? 'Not recorded' : 'Recorded history'],
        ['Fridge vehicle use', fridgeTruckUsePercent == null ? '—' : `${fridgeTruckUsePercent}%`, 'Published plan'],
      ].map(([label, value, note]) => <div key={label} className={card}><p className="text-xs font-bold uppercase tracking-wide text-muted">{label}</p><p className="mt-1 text-3xl font-bold text-brand">{value}</p><p className="mt-1 text-xs text-muted">{note}</p></div>)}
    </div>
    <div className="grid gap-5 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]">
      <section className={card}><h2 className="font-bold">Needs attention · {needsAttention.length}</h2><p className="mt-1 text-xs text-muted">Critical items first. Only recorded issues are shown.</p><div className="mt-4 space-y-3">{needsAttention.length === 0 && <p className="text-sm text-muted">No recorded items need attention.</p>}{needsAttention.map(item => <article key={item.id} className={`rounded-lg border p-4 ${item.severity === 'CRITICAL' ? 'border-status-failed bg-status-failed-soft' : 'border-status-risk bg-status-risk-soft'}`}><div className="flex items-start gap-2"><Icon name={item.severity === 'CRITICAL' ? 'crit' : 'warn'} className="size-5 shrink-0" /><div className="min-w-0"><h3 className="font-semibold">{item.title}</h3><p className="mt-1 text-sm">{item.details}</p>{item.orderId && <button className="mt-2 min-h-10 font-semibold underline" onClick={() => setSelectedOrder(item.orderId)}>{item.action}</button>}</div></div></article>)}</div></section>
      <section className={card}><h2 className="font-bold">Trips · {trips.length}</h2><p className="mt-1 text-xs text-muted">Progress comes from order statuses; last sync is unavailable until driver sync is recorded.</p><div className="mt-4 space-y-3">{trips.length === 0 && <p className="text-sm text-muted">No published trips for this run.</p>}{trips.map(trip => <article key={trip.id} className="rounded-lg border border-line bg-surface-2 p-4"><div className="flex justify-between gap-2"><h3 className="font-semibold">{trip.vehicleId} · T{trip.tripNo}</h3><span className="text-xs font-semibold text-brand">{statusText[trip.status] ?? trip.status}</span></div><p className="mt-1 text-sm">{trip.district} · {trip.stopsDone} of {trip.stopsTotal} stops complete</p><p className="mt-1 text-xs text-muted">{trip.lastUpdate ? `Last order update ${new Date(trip.lastUpdate).toLocaleString()}` : 'No order update'} · Sync unavailable</p></article>)}</div></section>
    </div>
    {selectedOrder && <div className="fixed inset-0 z-50 flex justify-end bg-overlay" role="dialog" aria-modal="true" aria-label="Order details"><button className="absolute inset-0" onClick={() => setSelectedOrder(null)} aria-label="Close order details" /><div className="relative h-full w-full max-w-lg overflow-auto bg-surface p-6"><button className="float-right min-h-10" onClick={() => setSelectedOrder(null)}>Close</button><h2 className="text-xl font-bold">Order details</h2>{detail.isPending && <p role="status">Loading…</p>}{detail.error && <p role="alert" className="text-danger">{detail.error.message}</p>}{detail.data && <><p className="mt-4 font-semibold">{detail.data.order.ref} · {detail.data.order.outletName}</p><p className="text-sm text-muted">{detail.data.order.status} · {detail.data.order.units} units</p><h3 className="mt-6 font-semibold">History</h3><ol className="mt-2 space-y-3">{detail.data.history.map((event, index) => <li key={index} className="border-t border-line pt-3 text-sm"><strong>{event.type}</strong> · {new Date(event.at).toLocaleString()}<p className="text-muted">{event.actor ?? 'System'}</p></li>)}</ol></>}</div></div>}
  </div>
}
