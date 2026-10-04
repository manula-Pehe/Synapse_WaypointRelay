import { Fragment, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { dispatchApi, type Order, type Vehicle } from './api'

const card = 'rounded-xl border border-line bg-surface p-5 text-ink shadow-sm'
const button = 'min-h-10 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-on-brand disabled:opacity-50'
const input = 'min-h-10 rounded-lg border border-line bg-surface px-3 py-2 text-sm text-ink'
const errorText = (error: unknown) => error instanceof Error ? error.message : 'Request failed'
type PhoneLine = { temp: 'CHILLED' | 'AMBIENT'; units: string }
const newPhoneLine = (): PhoneLine => ({ temp: 'AMBIENT', units: '' })

function QueryState({ loading, error }: { loading: boolean; error: unknown }) {
  if (loading) return <p role="status">Loading…</p>
  if (error) return <p role="alert" className="rounded-lg bg-danger-soft p-4 text-danger">{errorText(error)}</p>
  return null
}

export function DispatchOrders({ runDate, depot }: { runDate: string; depot: string }) {
  const client = useQueryClient()
  const [view, setView] = useState<'all' | 'unconfirmed'>('all')
  const [selected, setSelected] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)
  const [outletId, setOutletId] = useState('')
  const [lines, setLines] = useState<PhoneLine[]>([newPhoneLine()])
  const [note, setNote] = useState('')
  const [result, setResult] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [brandFilter, setBrandFilter] = useState('ALL')
  const [tempFilter, setTempFilter] = useState('ALL')
  const [outletFilter, setOutletFilter] = useState('')
  const key = ['dispatch-orders', runDate, depot]
  const orders = useQuery({ queryKey: [...key, 'list'], queryFn: () => dispatchApi.orders(runDate, depot) })
  const unconfirmed = useQuery({ queryKey: [...key, 'unconfirmed'], queryFn: () => dispatchApi.unconfirmed(runDate, depot) })
  const closeStatus = useQuery({ queryKey: [...key, 'close'], queryFn: () => dispatchApi.closeStatus(runDate, depot) })
  const detail = useQuery({ queryKey: [...key, 'detail', selected], queryFn: () => dispatchApi.order(selected!), enabled: !!selected })
  const close = useMutation({ mutationFn: () => dispatchApi.closeOrders(runDate, depot), onSuccess: data => { setResult(`Closed: ${data.confirmed} confirmed, ${data.autoConfirmed} auto-confirmed, ${data.notConfirmed} not confirmed.`); client.invalidateQueries({ queryKey: key }) } })
  const phoneIn = useMutation({ mutationFn: async () => {
    let created = 0
    for (const line of lines) {
      try {
        await dispatchApi.phoneIn({ outletId: outletId.trim(), runDate, temp: line.temp, units: Number(line.units), note })
        created++
      } catch (error) {
        setLines(current => current.slice(created))
        setResult(`${created} of ${lines.length} phone-in lines created. The remaining lines are still in the form.`)
        await client.invalidateQueries({ queryKey: key })
        throw error
      }
    }
    return created
  }, onSuccess: created => { setAdding(false); setLines([newPhoneLine()]); setResult(`${created} phone-in order ${created === 1 ? 'line' : 'lines'} created.`); client.invalidateQueries({ queryKey: key }) } })

  if (orders.isPending || closeStatus.isPending) return <QueryState loading error={null} />
  if (orders.error || closeStatus.error) return <QueryState loading={false} error={orders.error || closeStatus.error} />
  const list = orders.data.items
  const prepared = list.filter(order => order.status === 'PREPARED').length
  const chilled = list.filter(order => order.temp === 'CHILLED').length
  const visible = list.filter(order => (statusFilter === 'ALL' || order.status === statusFilter)
    && (brandFilter === 'ALL' || order.brand === brandFilter)
    && (tempFilter === 'ALL' || order.temp === tempFilter)
    && `${order.outletId} ${order.outletName}`.toLowerCase().includes(outletFilter.toLowerCase()))
  const statuses = [...new Set(list.map(order => order.status))].sort()
  const brands = [...new Set(list.map(order => order.brand))].sort()
  return <div className="space-y-5">
    <div className={card}>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div><h2 className="text-lg font-bold">Orders for {runDate} · {depot}</h2><p className="text-sm text-muted">{list.length} orders · {prepared} prepared · {chilled} chilled</p><p className="text-sm text-muted">{closeStatus.data.closed ? `Closed ${closeStatus.data.closedAt}` : `Open until ${closeStatus.data.cutOffAt}`}</p></div>
        <div className="flex gap-2"><button className={button} onClick={() => setAdding(true)}>Add phone-in order</button><button className={button} disabled={closeStatus.data.closed || close.isPending} onClick={() => { if (window.confirm(`Close orders for ${depot} on ${runDate}?`)) close.mutate() }}>Close orders</button></div>
      </div>
      {result && <p role="status" className="mt-3 text-emerald-700">{result}</p>}{close.error && <QueryState loading={false} error={close.error} />}
    </div>
    <div className="flex gap-2"><button className={button} onClick={() => setView('all')}>All ({list.length})</button><button className={button} onClick={() => setView('unconfirmed')}>Not confirmed ({unconfirmed.data?.total ?? '…'})</button></div>
    {view === 'all' ? <><div className={`${card} flex flex-wrap gap-3`} aria-label="Order filters"><label className="text-xs font-semibold">Status<select className={`ml-2 ${input}`} value={statusFilter} onChange={event => setStatusFilter(event.target.value)}><option value="ALL">All</option>{statuses.map(status => <option key={status}>{status}</option>)}</select></label><label className="text-xs font-semibold">Brand<select className={`ml-2 ${input}`} value={brandFilter} onChange={event => setBrandFilter(event.target.value)}><option value="ALL">All</option>{brands.map(brand => <option key={brand}>{brand}</option>)}</select></label><label className="text-xs font-semibold">Temperature<select className={`ml-2 ${input}`} value={tempFilter} onChange={event => setTempFilter(event.target.value)}><option value="ALL">All</option><option value="CHILLED">Chilled</option><option value="AMBIENT">Ambient</option></select></label><label className="text-xs font-semibold">Outlet<input className={`ml-2 ${input}`} placeholder="ID or name" value={outletFilter} onChange={event => setOutletFilter(event.target.value)} /></label></div><div className={`${card} overflow-x-auto`}><p className="mb-3 text-xs text-slate-500">Showing {visible.length} of {list.length} orders</p><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="p-2">Order</th><th>Outlet</th><th>Brand</th><th>Temperature</th><th>Units</th><th>Weight</th><th>Volume</th><th>Status</th><th>Source</th></tr></thead><tbody>{visible.map((order: Order) => <tr key={order.id} className="border-b"><td className="p-2"><button className="font-semibold text-blue-700 underline" onClick={() => setSelected(order.id)}>{order.ref}</button></td><td>{order.outletName}</td><td>{order.brand}</td><td><span className={order.temp === 'CHILLED' ? 'rounded bg-status-chilled-soft px-2 py-1 font-semibold text-status-chilled' : ''}>{order.temp}</span></td><td>{order.units}</td><td>{order.weightKg} kg</td><td>{order.volumeM3} m³</td><td>{order.status}</td><td>{order.source}</td></tr>)}</tbody></table>{visible.length === 0 && <p className="p-3">No orders match these filters.</p>}</div></>
      : <div className={`${card} overflow-x-auto`}><QueryState loading={unconfirmed.isPending} error={unconfirmed.error} />{unconfirmed.data && <table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="p-2">Outlet</th><th>Prepared orders</th><th>Phone</th><th>Action</th></tr></thead><tbody>{unconfirmed.data.items.map(store => <tr key={store.outletId} className="border-b"><td className="p-2">{store.outletName}</td><td>{store.orders.map(order => `${order.ref} · ${order.temp} · ${order.units} units`).join(', ')}</td><td>{store.phone ?? 'Not available'}</td><td className="space-x-3">{store.phone && <a className="font-semibold text-brand underline" href={`tel:${store.phone.replace(/[^+\d]/g, '')}`}>Call</a>}<button className="text-brand underline" onClick={() => { setOutletId(store.outletId); setAdding(true) }}>Enter by phone</button></td></tr>)}</tbody></table>}{unconfirmed.data?.total === 0 && <p className="p-3">All stores have confirmed.</p>}</div>}
    {selected && <div className="fixed inset-0 z-50 flex justify-end bg-slate-900/40"><div className="h-full w-full max-w-lg overflow-auto bg-white p-6"><button className="float-right min-h-10" onClick={() => setSelected(null)}>Close</button><h2 className="text-lg font-bold">Order details and history</h2><QueryState loading={detail.isPending} error={detail.error} />{detail.data && <><dl className="my-4 grid grid-cols-2 gap-3 rounded-xl bg-slate-50 p-4 text-sm">{Object.entries({ Reference: detail.data.order.ref, Outlet: detail.data.order.outletName, Brand: detail.data.order.brand, Temperature: detail.data.order.temp, Units: detail.data.order.units, Weight: `${detail.data.order.weightKg} kg`, Volume: `${detail.data.order.volumeM3} m³`, Status: detail.data.order.status, Source: detail.data.order.source }).map(([label, value]) => <div key={label}><dt className="text-xs text-slate-500">{label}</dt><dd className="font-semibold">{value}</dd></div>)}</dl><h3 className="font-semibold">Timeline</h3>{detail.data.history.length === 0 && <p className="text-sm text-slate-600">No history events.</p>}{detail.data.history.map((event, index) => <div key={index} className="border-t py-3 text-sm"><strong>{event.type}</strong> · {event.at}<p>{event.actor ?? 'System'} · {event.fromStatus ?? '—'} → {event.toStatus ?? '—'}</p>{Object.keys(event.details).length > 0 && <pre className="mt-1 overflow-auto whitespace-pre-wrap text-xs text-slate-600">{JSON.stringify(event.details, null, 2)}</pre>}</div>)}</>}</div></div>}
    {adding && <div className="fixed inset-0 z-50 flex justify-end bg-overlay"><form className="h-full w-full max-w-lg space-y-4 overflow-auto bg-surface p-6" onSubmit={event => { event.preventDefault(); phoneIn.mutate() }}><button type="button" className="float-right min-h-10" onClick={() => setAdding(false)}>Close</button><h2 className="text-lg font-bold">Add phone-in order</h2><p>Run date: {runDate}. Weight and volume are estimated by the server. Each line creates an order.</p><label className="block">Outlet ID<input className={`mt-1 block w-full ${input}`} required maxLength={10} value={outletId} onChange={event => setOutletId(event.target.value)} /></label><div className="space-y-3"><h3 className="font-semibold">Order lines</h3>{lines.map((line, index) => <div key={index} className="grid grid-cols-[1fr_1fr_auto] items-end gap-2"><label className="block text-sm">Temperature<select className={`mt-1 block w-full ${input}`} value={line.temp} onChange={event => setLines(current => current.map((item, i) => i === index ? { ...item, temp: event.target.value as PhoneLine['temp'] } : item))}><option value="AMBIENT">Ambient</option><option value="CHILLED">Chilled</option></select></label><label className="block text-sm">Units<input className={`mt-1 block w-full ${input}`} type="number" min="1" required value={line.units} onChange={event => setLines(current => current.map((item, i) => i === index ? { ...item, units: event.target.value } : item))} /></label><button type="button" className="min-h-10 px-2 text-danger disabled:opacity-40" disabled={lines.length === 1 || phoneIn.isPending} onClick={() => setLines(current => current.filter((_, i) => i !== index))} aria-label={`Remove line ${index + 1}`}>Remove</button></div>)}<button type="button" className="min-h-10 text-brand underline" disabled={phoneIn.isPending} onClick={() => setLines(current => [...current, newPhoneLine()])}>Add line</button></div><label className="block">Note<input className={`mt-1 block w-full ${input}`} maxLength={500} value={note} onChange={event => setNote(event.target.value)} /></label><button className={button} disabled={phoneIn.isPending}>{phoneIn.isPending ? 'Creating…' : `Create ${lines.length} ${lines.length === 1 ? 'line' : 'lines'}`}</button>{phoneIn.error && <><p role="status" className="text-sm text-muted">{result}</p><QueryState loading={false} error={phoneIn.error} /></>}</form></div>}
  </div>
}

export function DispatchFleet({ runDate, depot }: { runDate: string; depot: string }) {
  const client = useQueryClient()
  const [selected, setSelected] = useState<Vehicle | null>(null)
  const [reason, setReason] = useState('')
  const key = ['dispatch-fleet', runDate, depot]
  const fleet = useQuery({ queryKey: key, queryFn: () => dispatchApi.fleet(runDate, depot) })
  const confirm = useMutation({ mutationFn: () => dispatchApi.confirmFleet(runDate, depot), onSuccess: () => client.invalidateQueries({ queryKey: key }) })
  const update = useMutation({ mutationFn: (value: { vehicle: Vehicle; status: Vehicle['availability']; reason: string | null }) => dispatchApi.setAvailability(value.vehicle.id, runDate, value.status, value.reason), onSuccess: () => { setSelected(null); setReason(''); client.invalidateQueries({ queryKey: key }) } })
  if (fleet.isPending) return <QueryState loading error={null} />
  if (fleet.error) return <QueryState loading={false} error={fleet.error} />
  const grouped = fleet.data.items.reduce<Record<string, Vehicle[]>>((groups, vehicle) => {
    const name = `${vehicle.type} · ${vehicle.temp}`
    ;(groups[name] ??= []).push(vehicle)
    return groups
  }, {})
  return <div className="space-y-5">
    <div className={card}><div className="flex flex-wrap items-center justify-between gap-4"><div><h2 className="text-lg font-bold">Fleet · {depot} · {runDate}</h2><p className="text-sm text-slate-600">{fleet.data.counts.available} available · {fleet.data.counts.reeferAvailable} reefers · {fleet.data.counts.inWorkshop} in workshop · {fleet.data.counts.offRoad} off road</p><p className="text-sm text-slate-600">{fleet.data.confirmedAt ? `Confirmed ${fleet.data.confirmedAt}` : 'Awaiting confirmation'}</p></div><button className={button} disabled={confirm.isPending} onClick={() => confirm.mutate()}>Confirm fleet</button></div>{confirm.error && <QueryState loading={false} error={confirm.error} />}</div>
    {Object.entries(grouped).sort(([a], [b]) => a.localeCompare(b)).map(([group, vehicles]) => <Fragment key={group}><h3 className="text-base font-bold text-slate-800">{group} · {vehicles?.length ?? 0}</h3><div className={`${card} overflow-x-auto`}><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="p-2">Vehicle</th><th>Capacity</th><th>Fuel quota</th><th>Availability</th><th>Reason</th><th>Action</th></tr></thead><tbody>{vehicles?.map(vehicle => <tr key={vehicle.id} className="border-b"><td className="p-2 font-semibold">{vehicle.id}</td><td>{vehicle.weightCapKg} kg · {vehicle.volumeCapM3} m³</td><td>{vehicle.weeklyFuelQuotaL} L / week</td><td>{vehicle.availability.replaceAll('_', ' ')}</td><td>{vehicle.availabilityReason ?? '—'}</td><td><button className="min-h-10 text-blue-700 underline" onClick={() => { setSelected(vehicle); setReason(vehicle.availabilityReason ?? '') }}>Change</button></td></tr>)}</tbody></table></div></Fragment>)}
    {fleet.data.items.length === 0 && <p className={card}>No vehicles for this depot.</p>}
    {selected && <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4"><div className={`${card} w-full max-w-md space-y-4`}><h2 className="text-lg font-bold">Availability · {selected.id}</h2><p>This change applies to {runDate} only.</p><label className="block">Reason<input className={`mt-1 block w-full ${input}`} maxLength={200} value={reason} onChange={event => setReason(event.target.value)} /></label><div className="flex flex-wrap gap-2"><button className={button} disabled={update.isPending} onClick={() => update.mutate({ vehicle: selected, status: 'AVAILABLE', reason: null })}>Available</button><button className={button} disabled={update.isPending || !reason.trim()} onClick={() => update.mutate({ vehicle: selected, status: 'IN_WORKSHOP', reason: reason.trim() })}>In workshop</button><button className={button} disabled={update.isPending || !reason.trim()} onClick={() => update.mutate({ vehicle: selected, status: 'OFF_ROAD', reason: reason.trim() })}>Off road</button><button className="min-h-10 px-3" onClick={() => setSelected(null)}>Cancel</button></div>{update.error && <QueryState loading={false} error={update.error} />}</div></div>}
  </div>
}

export function DispatchOutlets({ depot }: { depot: string }) {
  const [search, setSearch] = useState('')
  const outlets = useQuery({ queryKey: ['dispatch-outlets', depot], queryFn: () => dispatchApi.outlets(depot) })
  if (outlets.isPending) return <QueryState loading error={null} />
  if (outlets.error) return <QueryState loading={false} error={outlets.error} />
  const shown = outlets.data.items.filter(outlet => `${outlet.id} ${outlet.name} ${outlet.brand} ${outlet.district}`.toLowerCase().includes(search.toLowerCase()))
  return <div className="space-y-5"><div className={card}><h2 className="text-lg font-bold">Outlets · {depot}</h2><p className="text-sm text-slate-600">{outlets.data.total} outlets</p><input className={`mt-4 w-full max-w-md ${input}`} aria-label="Search outlets" placeholder="Search outlets" value={search} onChange={event => setSearch(event.target.value)} /></div><div className={`${card} overflow-x-auto`}><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="p-2">Outlet</th><th>Brand</th><th>District</th><th>Dock</th><th>Access</th><th>Window</th></tr></thead><tbody>{shown.map(outlet => <tr key={outlet.id} className="border-b"><td className="p-2 font-semibold">{outlet.name}</td><td>{outlet.brand}</td><td>{outlet.district}</td><td>{outlet.dockType}</td><td>{outlet.parkingConstraint}</td><td>{outlet.mallWindowOpen && outlet.mallWindowClose ? `${outlet.mallWindowOpen}–${outlet.mallWindowClose} (mall)` : `${outlet.windowOpen}–${outlet.windowClose}`}</td></tr>)}</tbody></table>{shown.length === 0 && <p className="p-3">No outlets found.</p>}</div></div>
}
