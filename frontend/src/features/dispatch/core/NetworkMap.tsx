import { useQuery } from '@tanstack/react-query'
import { ApiError } from '../../../lib/api'
import { dispatchApi } from './api'
import { summarizeDistricts } from './networkModel'

export default function NetworkMap({ runDate, depot }: { runDate: string; depot: string }) {
  const plan = useQuery({ queryKey: ['dispatch-plan', runDate, depot], queryFn: () => dispatchApi.latestPlan(runDate, depot) })
  const orders = useQuery({ queryKey: ['dispatch-orders', runDate, depot, 'list'], queryFn: () => dispatchApi.orders(runDate, depot) })
  if (plan.isPending || orders.isPending) return <p role="status">Loading district trips…</p>
  if (plan.error instanceof ApiError && plan.error.status === 404) return <div className="rounded-xl border border-slate-200 bg-white p-6"><h2 className="font-bold">No plan yet</h2><p className="mt-2 text-sm text-slate-600">Create a plan to see trips by district.</p></div>
  if (plan.error || orders.error) return <p role="alert" className="rounded-xl bg-red-50 p-5 text-red-800">{(plan.error || orders.error)?.message}</p>
  const rows = summarizeDistricts(plan.data, orders.data.items)
  const cx = 350, cy = 260
  return <div className="grid gap-5 lg:grid-cols-2">
    <div className="rounded-xl border border-slate-200 bg-white p-5"><h2 className="font-bold">{depot} district network</h2><p className="mt-1 text-xs text-slate-500">Schematic connections from depot to district. No vehicle positions are shown.</p><svg viewBox="0 0 700 520" role="img" aria-label={`Schematic of ${rows.length} districts served from ${depot}`} className="mt-4 w-full">
      {rows.map((row, index) => { const angle = (index / Math.max(rows.length, 1)) * Math.PI * 2 - Math.PI / 2; const x = cx + Math.cos(angle) * 245; const y = cy + Math.sin(angle) * 180; const color = row.states['On road'] ? '#d97706' : row.states.Completed === row.trips ? '#00856f' : '#1f4f87'; return <g key={row.district}><line x1={cx} y1={cy} x2={x} y2={y} stroke="#b8c6d5" strokeWidth="2" /><circle cx={x} cy={y} r="34" fill={color} /><text x={x} y={y + 4} textAnchor="middle" fill="white" fontSize="15" fontWeight="700">{row.trips}</text><text x={x} y={y + (y < cy ? -45 : 55)} textAnchor="middle" fontSize="13" fontWeight="600" fill="#17212d">{row.district}</text></g> })}
      <circle cx={cx} cy={cy} r="46" fill="#0e2a47" /><text x={cx} y={cy + 4} textAnchor="middle" fill="white" fontSize="13" fontWeight="700">Depot</text>
    </svg></div>
    <div className="rounded-xl border border-slate-200 bg-white p-5"><h2 className="font-bold">Trip status by district</h2><p className="mt-1 text-xs text-slate-500">Status is derived from the plan and its orders.</p>{rows.length === 0 ? <p className="mt-4 text-sm text-slate-600">No trips in this plan.</p> : <div className="mt-4 overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="py-2">District</th><th>Trips</th><th>Stops</th><th>Status</th></tr></thead><tbody>{rows.map(row => <tr key={row.district} className="border-b"><td className="py-3 font-semibold">{row.district}</td><td>{row.trips}</td><td>{row.stops}</td><td>{Object.entries(row.states).filter(([, count]) => count > 0).map(([state, count]) => `${count} ${state.toLowerCase()}`).join(' · ')}</td></tr>)}</tbody></table></div>}</div>
  </div>
}
