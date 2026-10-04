import { useQuery } from '@tanstack/react-query'
import { dispatchApi, type RunReport as RunReportData } from '../features/dispatch/core/api'

const card = 'rounded-xl border border-line bg-surface p-5'
const csvCell = (value: string | number | null) => {
  const text = String(value ?? '')
  const safe = /^[=+@\-\t\r]/.test(text) ? `'${text}` : text
  return `"${safe.replaceAll('"', '""')}"`
}

function exportCsv(report: RunReportData) {
  const rows: (string | number | null)[][] = [
    ['Run date', report.runDate], ['Depot', report.depot],
    ['On time percent', report.onTimePercent], ['On time stops', report.onTimeStops],
    ['Completed stops', report.completedStops], ['Deferred', report.deferred],
    ['Failed', report.failed], ['Partial', report.partial], [],
    ['District', 'Late stops', 'Completed stops'],
    ...report.lateByDistrict.map(row => [row.district, row.late, row.completed]),
    [], ['Exception type', 'Detail', 'Order ID'],
    ...report.exceptions.map(item => [item.type, item.detail, item.orderId]),
  ]
  const blob = new Blob([rows.map(row => row.map(csvCell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `dispatch-run-${report.runDate}-${report.depot}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

export default function RunReport({ runDate, depot }: { runDate: string; depot: string }) {
  const query = useQuery({ queryKey: ['dispatch-report', runDate, depot], queryFn: () => dispatchApi.runReport(runDate, depot) })
  if (query.isPending) return <p role="status">Loading run report…</p>
  if (query.error) return <p role="alert" className="rounded-xl bg-danger-soft p-5 text-danger">Could not load report: {query.error.message}</p>
  const report = query.data
  return <div className="space-y-5 text-ink">
    <div className="flex flex-wrap items-center justify-between gap-3"><p className="text-sm text-muted">Recorded results · {depot} · {runDate}</p><button className="min-h-10 rounded-lg border border-line bg-surface px-4 text-sm font-semibold" onClick={() => exportCsv(report)}>Export CSV</button></div>
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      {[
        ['On time', report.onTimePercent == null ? '—' : `${report.onTimePercent}%`, `${report.onTimeStops} of ${report.completedStops} completed stops`],
        ['Deferred', report.deferred == null ? '—' : String(report.deferred), report.deferred == null ? 'No published plan' : 'Published plan'],
        ['Failed', String(report.failed), 'Recorded order outcomes'],
        ['Partial', String(report.partial), 'Recorded order outcomes'],
      ].map(([label, value, note]) => <div key={label} className={card}><p className="text-xs font-bold uppercase tracking-wide text-muted">{label}</p><p className="mt-1 text-3xl font-bold text-brand">{value}</p><p className="mt-1 text-xs text-muted">{note}</p></div>)}
    </div>
    <div className="grid gap-5 lg:grid-cols-2">
      <section className={card}><h2 className="font-bold">Late deliveries by district</h2><p className="mt-1 text-xs text-muted">Based on recorded outcome times and planned arrival windows.</p><div className="mt-4 space-y-4">{report.lateByDistrict.length === 0 && <p className="text-sm text-muted">No published trip stops.</p>}{report.lateByDistrict.map(row => <div key={row.district} className="grid grid-cols-[7rem_1fr_auto] items-center gap-3 text-sm"><span>{row.district}</span><div className="h-3 rounded-full bg-surface-2"><div className="h-3 rounded-full bg-status-risk" style={{ width: `${row.completed ? (100 * row.late / row.completed) : 0}%` }} /></div><strong>{row.late} of {row.completed}</strong></div>)}</div></section>
      <section className={card}><h2 className="font-bold">Recorded exceptions · {report.exceptions.length}</h2><p className="mt-1 text-xs text-muted">Failed and partial outcomes, plus deferrals awaiting a decision.</p><div className="mt-4 space-y-3">{report.exceptions.length === 0 && <p className="text-sm text-muted">No recorded exceptions.</p>}{report.exceptions.map((item, index) => <div key={`${item.type}-${item.orderId}-${index}`} className="rounded-lg bg-surface-2 p-3 text-sm"><strong>{item.type.replaceAll('_', ' ')}</strong><p className="mt-1 text-muted">{item.detail}</p></div>)}</div></section>
    </div>
  </div>
}
