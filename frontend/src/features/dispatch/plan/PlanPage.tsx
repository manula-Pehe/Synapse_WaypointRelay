import { useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../../lib/api'
import {
  dispatchApi,
  type Plan,
  type PlanDeferral,
  type PlanTrip,
  type PlanVehicle,
} from '../core/api'

type Tab = 'board' | 'deferrals'
const card = 'rounded-xl border border-line bg-surface p-5 text-ink shadow-sm'
const primary =
  'min-h-11 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-on-brand disabled:cursor-not-allowed disabled:opacity-50'
const secondary =
  'min-h-11 rounded-lg border border-brand bg-surface px-4 py-2 text-sm font-semibold text-brand disabled:cursor-not-allowed disabled:opacity-50'
const errorText = (error: unknown) => (error instanceof Error ? error.message : 'Request failed')
const dateText = (value: string) =>
  new Date(`${value}T12:00:00`).toLocaleDateString('en-LK', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  })
const timeText = (value: string) =>
  new Date(value).toLocaleTimeString('en-LK', {
    hour: 'numeric',
    minute: '2-digit',
    timeZone: 'Asia/Colombo',
  })
const compact = (value: number, digits = 0) =>
  value.toLocaleString('en-LK', { maximumFractionDigits: digits })
const percent = (used: number, cap: number) => (cap > 0 ? Math.min(100, (100 * used) / cap) : 0)

function ErrorMessage({ error }: { error: unknown }) {
  return error ? (
    <p role="alert" className="rounded-lg bg-danger-soft p-4 text-sm text-danger">
      {errorText(error)}
    </p>
  ) : null
}

function Stat({
  label,
  value,
  tone = 'green',
}: {
  label: string
  value: number
  tone?: 'green' | 'pink' | 'blue' | 'red'
}) {
  const colors = {
    green: 'bg-emerald-100 text-emerald-800',
    pink: 'bg-pink-100 text-pink-800',
    blue: 'bg-blue-100 text-blue-800',
    red: 'bg-red-100 text-red-800',
  }
  return (
    <span className={`inline-flex rounded-full px-3 py-1 text-sm font-semibold ${colors[tone]}`}>
      {value} {label}
    </span>
  )
}

function Meter({
  label,
  used,
  cap,
  unit,
}: {
  label: string
  used: number
  cap: number
  unit: string
}) {
  const full = cap > 0 && used / cap > 0.9
  return (
    <div className="mt-3 text-xs">
      <div className="flex justify-between gap-2">
        <span>{label}</span>
        <strong>
          {compact(used, unit === 'm³' ? 1 : 0)} / {compact(cap, unit === 'm³' ? 1 : 0)} {unit}
        </strong>
      </div>
      <div className="mt-1 h-1.5 rounded-full bg-slate-200">
        <div
          className={`h-1.5 rounded-full ${full ? 'bg-amber-600' : 'bg-brand'}`}
          style={{ width: `${percent(used, cap)}%` }}
        />
      </div>
    </div>
  )
}

function TripCard({ trip, vehicle }: { trip: PlanTrip; vehicle: PlanVehicle }) {
  const chilled = trip.stops.some((stop) => stop.temp === 'CHILLED')
  return (
    <details
      className={`min-w-0 rounded-xl border p-3 ${chilled ? 'border-sky-200 bg-sky-50' : 'border-line bg-white'}`}
    >
      <summary className="cursor-pointer list-none focus-visible:outline-2 focus-visible:outline-brand">
        <div className="flex items-center justify-between gap-2">
          <strong className="text-sm">
            Trip {trip.tripNo} · {trip.district}
          </strong>
          <span className="rounded border border-current px-1.5 py-0.5 text-[10px] font-bold text-sky-700">
            {trip.windowType === 'FRESH' ? 'PRE-DAWN' : 'DAYTIME'}
          </span>
        </div>
        <p className="mt-2 text-xs text-muted">
          {trip.brand} {chilled ? '❄' : ''} · {trip.stops.length}{' '}
          {trip.stops.length === 1 ? 'stop' : 'stops'} ·{' '}
          {trip.stops.map((stop) => stop.outletId).join(', ')}
        </p>
        <p className="mt-1 text-xs text-muted">
          Departs {timeText(trip.departAt)} · {trip.minutes} min
        </p>
        <Meter label="Weight" used={trip.weightKg} cap={vehicle.weightCapKg} unit="kg" />
        <Meter label="Volume" used={trip.volumeM3} cap={vehicle.volumeCapM3} unit="m³" />
        <span className="mt-2 block text-xs font-medium text-brand">
          View stops and arrival windows
        </span>
      </summary>
      <ol className="mt-3 space-y-2 border-t border-line pt-3 text-xs">
        {[...trip.stops]
          .sort((a, b) => a.seq - b.seq)
          .map((stop) => (
            <li
              key={stop.id}
              className="flex flex-wrap justify-between gap-2 rounded-lg bg-white/80 px-3 py-2"
            >
              <span>
                <strong>
                  {stop.seq}. {stop.outletId}
                </strong>{' '}
                · {stop.orderRef} · {stop.units} cases {stop.temp === 'CHILLED' ? '❄' : ''}
              </span>
              <span>
                {timeText(stop.arriveFrom)}–{timeText(stop.arriveTo)} · load #{stop.loadSeq}
              </span>
            </li>
          ))}
      </ol>
    </details>
  )
}

function PlanBoard({ plan }: { plan: Plan }) {
  return (
    <div className="space-y-4">
      <div className="hidden grid-cols-[11rem_minmax(0,1fr)_minmax(0,1fr)_9rem] gap-3 px-1 text-xs font-semibold text-muted lg:grid">
        <span>Vehicle</span>
        <span>Trip 1</span>
        <span>Trip 2</span>
        <span>Time used</span>
      </div>
      {plan.vehicles.map((vehicle) => (
        <section
          key={vehicle.vehicleId}
          className="grid gap-3 rounded-xl border border-line bg-surface p-3 lg:grid-cols-[11rem_minmax(0,1fr)_minmax(0,1fr)_9rem]"
        >
          <div>
            <h3 className="font-bold">{vehicle.vehicleId}</h3>
            <p className="text-xs text-muted">
              {vehicle.type} · {vehicle.temp}
            </p>
            <p className="mt-1 text-xs text-muted">
              {compact(vehicle.weightCapKg)} kg · {compact(vehicle.volumeCapM3, 1)} m³
            </p>
          </div>
          {[1, 2].map((number) => (
            <div key={number}>
              {vehicle.trips.find((trip) => trip.tripNo === number) ? (
                <TripCard
                  trip={vehicle.trips.find((trip) => trip.tripNo === number)!}
                  vehicle={vehicle}
                />
              ) : (
                <div className="flex h-full min-h-24 items-center justify-center rounded-xl border border-dashed border-line text-xs text-muted">
                  No trip {number}
                </div>
              )}
            </div>
          ))}
          <div>
            <Meter
              label="Pre-dawn"
              used={vehicle.freshMinutesUsed}
              cap={vehicle.freshBudget}
              unit="min"
            />
            <Meter
              label="Daytime"
              used={vehicle.daytimeMinutesUsed}
              cap={vehicle.daytimeBudget}
              unit="min"
            />
          </div>
        </section>
      ))}
      {plan.vehicles.length === 0 && (
        <p className={card}>
          No vehicle trips were allocated. Review the deferrals before publishing.
        </p>
      )}
    </div>
  )
}

function Deferrals({ planId, count }: { planId: string; count: number }) {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const query = useQuery({
    queryKey: ['dispatch-plan-deferrals', planId],
    queryFn: () => dispatchApi.planDeferrals(planId),
  })
  if (query.isPending) return <p role="status">Loading deferrals…</p>
  if (query.error) return <ErrorMessage error={query.error} />
  const rows = query.data.items
  const selected: PlanDeferral | undefined = rows.find((row) => row.id === selectedId) ?? rows[0]
  return (
    <div className="space-y-4">
      <div className={card}>
        <div className="flex flex-wrap items-center gap-3">
          <Stat
            value={rows.filter((row) => row.kind === 'UNAVOIDABLE').length}
            label="unavoidable"
            tone="red"
          />
          <Stat
            value={rows.filter((row) => row.kind === 'CHOSEN').length}
            label="chosen"
            tone="pink"
          />
          <span className="text-sm text-muted">
            {count} orders deferred · each has a recorded reason
          </span>
        </div>
      </div>
      {rows.length === 0 ? (
        <div className={card}>No deferrals in this plan.</div>
      ) : (
        <div className="grid gap-4 xl:grid-cols-[minmax(0,1.4fr)_minmax(20rem,0.8fr)]">
          <div className="space-y-2">
            {rows.map((row) => (
              <button
                type="button"
                key={row.id}
                onClick={() => setSelectedId(row.id)}
                className={`w-full rounded-xl border p-4 text-left text-sm ${selected?.id === row.id ? 'border-brand bg-blue-50' : 'border-line bg-surface'}`}
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <strong>
                    {row.orderRef} · {row.outletId}
                  </strong>
                  <span
                    className={`rounded-full px-2 py-1 text-xs font-semibold ${row.kind === 'UNAVOIDABLE' ? 'bg-red-100 text-red-800' : 'bg-pink-100 text-pink-800'}`}
                  >
                    {row.kind === 'CHOSEN' ? 'Chosen' : 'Unavoidable'}
                  </span>
                </div>
                <p className="mt-1 text-xs text-muted">
                  {row.rule.replaceAll('_', ' ')} · waited {row.daysWaited}{' '}
                  {row.daysWaited === 1 ? 'day' : 'days'} · priority {row.priorityScore}
                </p>
                <span className="mt-2 block text-xs font-semibold text-brand">Why? →</span>
              </button>
            ))}
          </div>
          {selected && (
            <aside className={`${card} self-start border-brand`}>
              <h3 className="text-lg font-bold">Why is {selected.orderRef} deferred?</h3>
              <p className="mt-2 text-sm font-semibold text-pink-800">
                {selected.kind === 'CHOSEN'
                  ? 'Chosen - capacity or timing decision'
                  : 'Unavoidable - operating rule'}
              </p>
              <p className="mt-4 whitespace-pre-wrap text-sm leading-6">{selected.reason}</p>
              <dl className="mt-4 grid grid-cols-2 gap-3 rounded-lg bg-surface-2 p-3 text-sm">
                <div>
                  <dt className="text-xs text-muted">Rule</dt>
                  <dd className="font-semibold">{selected.rule.replaceAll('_', ' ')}</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted">Priority score</dt>
                  <dd className="font-semibold">{selected.priorityScore}</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted">Days waited</dt>
                  <dd className="font-semibold">{selected.daysWaited}</dd>
                </div>
                <div>
                  <dt className="text-xs text-muted">New date</dt>
                  <dd className="font-semibold">{dateText(selected.newDate)}</dd>
                </div>
              </dl>
              {selected.needsDecision && (
                <p className="mt-3 text-sm font-medium text-amber-800">
                  This order needs a dispatcher decision.
                </p>
              )}
              {selected.storeChoice && (
                <p className="mt-3 text-sm">Store choice: {selected.storeChoice}</p>
              )}
            </aside>
          )}
        </div>
      )}
    </div>
  )
}

export function PlanPage({
  runDate,
  depot,
  onNavigate,
}: {
  runDate: string
  depot: string
  onNavigate: (page: string) => void
}) {
  const client = useQueryClient()
  const [tab, setTab] = useState<Tab>('board')
  const [confirmPublish, setConfirmPublish] = useState(false)
  const [confirmReplan, setConfirmReplan] = useState(false)
  const key = ['dispatch-plan-status', runDate, depot]
  const planQuery = useQuery({
    queryKey: key,
    queryFn: () => dispatchApi.latestPlan(runDate, depot),
    refetchOnMount: 'always',
  })
  const readiness = useQuery({
    queryKey: ['dispatch-plan-readiness', runDate, depot],
    queryFn: () => dispatchApi.planReadiness(runDate, depot),
    enabled: planQuery.error instanceof ApiError && planQuery.error.status === 404,
    refetchOnMount: 'always',
  })
  const refresh = async () => {
    await client.invalidateQueries({ queryKey: ['dispatch-plan-status', runDate, depot] })
    await client.invalidateQueries({ queryKey: ['dispatch-plan-readiness', runDate, depot] })
    await client.invalidateQueries({ queryKey: ['dispatch-plan-deferrals'] })
  }
  const create = useMutation({
    mutationFn: () => dispatchApi.createPlan(runDate, depot),
    onSuccess: async () => {
      setConfirmReplan(false)
      setTab('board')
      await refresh()
    },
  })
  const publish = useMutation({
    mutationFn: (id: string) => dispatchApi.publishPlan(id),
    onSuccess: async () => {
      setConfirmPublish(false)
      await refresh()
      await client.invalidateQueries({ queryKey: ['dispatch-live'] })
      await client.invalidateQueries({ queryKey: ['dispatch-notifications'] })
    },
  })

  if (planQuery.isPending) return <p role="status">Checking the plan for {dateText(runDate)}…</p>
  if (planQuery.error && !(planQuery.error instanceof ApiError && planQuery.error.status === 404))
    return <ErrorMessage error={planQuery.error} />
  const plan = planQuery.data
  if (!plan) {
    if (readiness.isPending) return <p role="status">Checking plan readiness…</p>
    if (readiness.error) return <ErrorMessage error={readiness.error} />
    const state = readiness.data
    const canCreate =
      state.ordersClosed &&
      state.fleetConfirmed &&
      state.confirmedOrders > 0 &&
      state.availableVehicles > 0
    return (
      <div className="max-w-4xl space-y-4">
        <section className={card}>
          <h2 className="text-xl font-bold">Ready to create the {dateText(runDate)} plan?</h2>
          <p className="mt-1 text-sm text-muted">
            The plan uses confirmed orders and the fleet recorded for {depot}. Review the checks
            before creating it.
          </p>
          <div className="mt-6 space-y-5">
            <ReadinessLine
              good={state.ordersClosed}
              title={state.ordersClosed ? 'Orders closed' : 'Orders must be closed'}
              detail={`${state.confirmedOrders} confirmed orders`}
              action={
                !state.ordersClosed ? (
                  <button className={secondary} onClick={() => onNavigate('orders')}>
                    Review orders
                  </button>
                ) : undefined
              }
            />
            <ReadinessLine
              good={state.fleetConfirmed}
              title={state.fleetConfirmed ? 'Fleet confirmed' : 'Fleet not confirmed'}
              detail={`${state.availableVehicles} available vehicles · ${state.reeferAvailable} fridge vehicles`}
              action={
                !state.fleetConfirmed ? (
                  <button className={secondary} onClick={() => onNavigate('fleet')}>
                    Confirm fleet
                  </button>
                ) : undefined
              }
            />
            <ReadinessLine
              good={state.confirmedOrders > 0 && state.availableVehicles > 0}
              title="Planning inputs"
              detail={`${state.confirmedOrders} orders for ${state.availableVehicles} vehicles`}
            />
          </div>
          {state.warnings.length > 0 && (
            <ul className="mt-5 list-inside list-disc text-sm text-amber-800">
              {state.warnings.map((warning) => (
                <li key={warning}>{warning}</li>
              ))}
            </ul>
          )}
          <div className="mt-7 flex flex-wrap items-center gap-3">
            <button
              className={primary}
              disabled={!canCreate || create.isPending}
              onClick={() => create.mutate()}
            >
              {create.isPending ? 'Creating plan…' : 'Create plan'}
            </button>
            {!canCreate && (
              <span className="text-sm text-danger">Complete the required checks first.</span>
            )}
          </div>
          <div className="mt-3">
            <ErrorMessage error={create.error} />
          </div>
        </section>
      </div>
    )
  }
  const published = plan.status === 'PUBLISHED'
  const trips = plan.vehicles.flatMap((vehicle) => vehicle.trips)
  const stops = trips.flatMap((trip) => trip.stops)
  return (
    <div className="space-y-4">
      {published && (
        <div
          role="status"
          className="rounded-xl border border-emerald-600 bg-emerald-50 p-4 text-sm text-emerald-900"
        >
          <strong>
            Published · {plan.summary.served} orders got arrival windows · {plan.summary.deferred}{' '}
            got deferral notices with reasons.
          </strong>
          <p className="mt-1">Loaders and drivers can now read their trips.</p>
        </div>
      )}
      <section className={card}>
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-wrap gap-2">
            <Stat value={plan.summary.served} label="served" />
            <Stat value={plan.summary.deferred} label="deferred" tone="pink" />
            <Stat
              value={plan.summary.violations}
              label="rule violations"
              tone={plan.summary.violations ? 'red' : 'green'}
            />
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div
              className="flex rounded-lg bg-surface-2 p-1"
              role="tablist"
              aria-label="Plan views"
            >
              <button
                className={`min-h-10 rounded-md px-3 text-sm ${tab === 'board' ? 'bg-surface font-semibold shadow-sm' : ''}`}
                role="tab"
                aria-selected={tab === 'board'}
                onClick={() => setTab('board')}
              >
                Board
              </button>
              <button
                className="min-h-10 rounded-md px-3 text-sm"
                onClick={() => onNavigate('network-map')}
                title={
                  published
                    ? 'Open the published trip map'
                    : 'Trip map is available after publishing'
                }
                disabled={!published}
              >
                Map
              </button>
              <button
                className={`min-h-10 rounded-md px-3 text-sm ${tab === 'deferrals' ? 'bg-surface font-semibold shadow-sm' : ''}`}
                role="tab"
                aria-selected={tab === 'deferrals'}
                onClick={() => setTab('deferrals')}
              >
                Deferrals ({plan.summary.deferred})
              </button>
            </div>
            {published ? (
              <>
                <button
                  className={secondary}
                  disabled
                  title="Plan v2 editing is not available in this workflow"
                >
                  Make changes (v2)
                </button>
                <button className={primary} onClick={() => onNavigate('live-board')}>
                  Open live board
                </button>
              </>
            ) : (
              <>
                <button
                  className={secondary}
                  disabled={create.isPending}
                  onClick={() => setConfirmReplan(true)}
                >
                  Re-plan
                </button>
                <button
                  className={primary}
                  disabled={publish.isPending || plan.summary.violations > 0}
                  onClick={() => setConfirmPublish(true)}
                >
                  Publish plan v{plan.version}
                </button>
              </>
            )}
          </div>
        </div>
        <p className="mt-4 text-xs text-muted">
          Plan v{plan.version} · {published ? 'published and read-only' : 'draft'} ·{' '}
          {plan.vehicles.length} vehicles · {trips.length} trips · {stops.length} stops ·{' '}
          {plan.summary.fridgeVehiclesUsed}/{plan.summary.fridgeVehiclesAvailable} fridge vehicles
          used
        </p>
        <ErrorMessage error={create.error || publish.error} />
      </section>
      {tab === 'board' ? (
        <PlanBoard plan={plan} />
      ) : (
        <Deferrals planId={plan.id} count={plan.summary.deferred} />
      )}
      {confirmReplan && (
        <Dialog title="Replace this draft plan?" onClose={() => setConfirmReplan(false)}>
          <p className="text-sm text-muted">
            The current draft will be replaced using the latest confirmed orders and fleet. Review
            the new board and deferrals before publishing.
          </p>
          <div className="mt-5 flex gap-2">
            <button className={secondary} onClick={() => setConfirmReplan(false)}>
              Keep draft
            </button>
            <button className={primary} disabled={create.isPending} onClick={() => create.mutate()}>
              {create.isPending ? 'Creating…' : 'Create new draft'}
            </button>
          </div>
          <ErrorMessage error={create.error} />
        </Dialog>
      )}
      {confirmPublish && (
        <Dialog title={`Publish plan v${plan.version}?`} onClose={() => setConfirmPublish(false)}>
          <p className="text-sm text-muted">
            {plan.summary.served} orders on {plan.vehicles.length} vehicles ({trips.length} trips) ·{' '}
            {plan.summary.deferred} orders deferred with reasons. Publishing locks this version.
          </p>
          <div className="mt-4 rounded-lg bg-surface-2 p-4 text-sm">
            <strong>Who is notified now</strong>
            <ul className="mt-2 list-inside list-disc space-y-1">
              <li>Loaders at the {depot} dock - loading lists ready</li>
              <li>Drivers of the {plan.vehicles.length} used vehicles - trips ready</li>
              <li>Stores with planned orders - arrival windows</li>
              <li>Stores with deferred orders - new date and reason</li>
            </ul>
          </div>
          <div className="mt-5 flex gap-2">
            <button className={secondary} onClick={() => setConfirmPublish(false)}>
              Keep editing
            </button>
            <button
              className={primary}
              disabled={publish.isPending}
              onClick={() => publish.mutate(plan.id)}
            >
              {publish.isPending ? 'Publishing…' : `Publish plan v${plan.version}`}
            </button>
          </div>
          <ErrorMessage error={publish.error} />
        </Dialog>
      )}
    </div>
  )
}

function ReadinessLine({
  good,
  title,
  detail,
  action,
}: {
  good: boolean
  title: string
  detail: string
  action?: ReactNode
}) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <span
        className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-lg ${good ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-700'}`}
      >
        {good ? '✓' : '!'}
      </span>
      <div className="min-w-40 flex-1">
        <strong className="text-sm">{title}</strong>
        <p className="text-sm text-muted">{detail}</p>
      </div>
      {action}
    </div>
  )
}

function Dialog({
  title,
  onClose,
  children,
}: {
  title: string
  onClose: () => void
  children: ReactNode
}) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="w-full max-w-lg rounded-2xl bg-surface p-6 shadow-xl"
      >
        <h2 className="text-xl font-bold">{title}</h2>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  )
}
