import { useQuery } from '@tanstack/react-query'
import { ApiError } from '../../../lib/api'
import { dispatchApi } from './api'
import { summarizeDistricts } from './networkModel'

export default function NetworkMap({ runDate, depot }: { runDate: string; depot: string }) {
  const plan = useQuery({ queryKey: ['dispatch-plan', runDate, depot], queryFn: () => dispatchApi.latestPlan(runDate, depot) })
  const live = useQuery({ queryKey: ['dispatch-live', runDate, depot], queryFn: () => dispatchApi.live(runDate, depot), refetchInterval: 15_000 })
  if (plan.isPending || live.isPending) return <p role="status">Loading district trips…</p>
  if (plan.error instanceof ApiError && plan.error.status === 404) return <div className="rounded-xl border border-line bg-surface p-6"><h2 className="font-bold">No plan yet</h2><p className="mt-2 text-sm text-muted">Create and publish a plan to see trips by district.</p></div>
  if (plan.error || live.error) return <p role="alert" className="rounded-xl bg-danger-soft p-5 text-danger">{(plan.error || live.error)?.message}</p>
  if (plan.data.status !== 'PUBLISHED') return <div className="rounded-xl border border-line bg-surface p-6"><h2 className="font-bold">Plan not published</h2><p className="mt-2 text-sm text-muted">Trip status appears after this plan is published.</p></div>
  const rows = summarizeDistricts(live.data.trips)
  const cx = 350, cy = 260
  return <div className="grid gap-5 lg:grid-cols-2">
    <div className="rounded-xl border border-line bg-surface p-5"><h2 className="font-bold">{depot} district network</h2><p className="mt-1 text-xs text-muted">Schematic connections from depot to district. No vehicle positions are shown.</p><svg viewBox="0 0 700 520" role="img" aria-label={`Schematic of ${rows.length} districts served from ${depot}`} className="mt-4 w-full">
      {rows.map((row, index) => { const angle = (index / Math.max(rows.length, 1)) * Math.PI * 2 - Math.PI / 2; const x = cx + Math.cos(angle) * 245; const y = cy + Math.sin(angle) * 180; const color = row.states['On road'] ? 'var(--color-status-risk)' : row.states.Completed === row.trips ? 'var(--color-status-delivered)' : 'var(--color-brand)'; return <g key={row.district}><line x1={cx} y1={cy} x2={x} y2={y} stroke="var(--color-line)" strokeWidth="2" /><circle cx={x} cy={y} r="34" fill={color} /><text x={x} y={y + 4} textAnchor="middle" fill="white" fontSize="15" fontWeight="700">{row.trips}</text><text x={x} y={y + (y < cy ? -45 : 55)} textAnchor="middle" fontSize="13" fontWeight="600" fill="var(--color-ink)">{row.district}</text></g> })}
      <circle cx={cx} cy={cy} r="46" fill="var(--color-brand)" /><text x={cx} y={cy + 4} textAnchor="middle" fill="white" fontSize="13" fontWeight="700">Depot</text>
    </svg></div>
    <div className="rounded-xl border border-line bg-surface p-5"><h2 className="font-bold">Trip status by district</h2><p className="mt-1 text-xs text-muted">Counts and progress use the live dispatch response, refreshed every 15 seconds.</p>{rows.length === 0 ? <p className="mt-4 text-sm text-muted">No trips in this published plan.</p> : <div className="mt-4 overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b border-line"><th className="py-2">District</th><th>Trips</th><th>Stops done</th><th>Status</th></tr></thead><tbody>{rows.map(row => <tr key={row.district} className="border-b border-line"><td className="py-3 font-semibold">{row.district}</td><td>{row.trips}</td><td>{row.done} of {row.stops}</td><td>{Object.entries(row.states).filter(([, count]) => count > 0).map(([state, count]) => `${count} ${state.toLowerCase()}`).join(' · ')}</td></tr>)}</tbody></table></div>}</div>
  </div>
}
