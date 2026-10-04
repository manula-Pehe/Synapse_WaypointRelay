import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../lib/api'
import { LoaderLayout } from './LoaderLayout'
import { loaderApi, type StopDetail, type TripDetail, type TripSummary } from './api'

const time = (value: string) =>
  new Date(value).toLocaleTimeString('en-LK', {
    timeZone: 'Asia/Colombo',
    hour: 'numeric',
    minute: '2-digit',
  })
const button =
  'min-h-14 rounded-xl px-5 font-semibold disabled:cursor-not-allowed disabled:opacity-50'
const primary = `${button} bg-brand text-on-brand`
const secondary = `${button} border border-brand bg-surface text-brand`
const errorText = (error: unknown) =>
  error instanceof ApiError ? error.message : 'Something went wrong. Please try again.'

export function LoaderHome() {
  const [tripId, setTripId] = useState<string | null>(null)
  const [screen, setScreen] = useState<'fridge' | 'loading' | 'handover' | 'complete'>('loading')
  const [shortfallStop, setShortfallStop] = useState<StopDetail | null>(null)
  const [notice, setNotice] = useState('')
  const client = useQueryClient()
  const trips = useQuery({
    queryKey: ['loader-trips'],
    queryFn: loaderApi.trips,
    refetchInterval: 15_000,
  })
  const detail = useQuery({
    queryKey: ['loader-trip', tripId],
    queryFn: () => loaderApi.trip(tripId!),
    enabled: !!tripId,
    refetchInterval: 15_000,
  })
  const refresh = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ['loader-trips'] }),
      client.invalidateQueries({ queryKey: ['loader-trip', tripId] }),
    ])
  }
  const selectTrip = (trip: TripSummary) => {
    if (!trip.vehicleAvailable) return
    setTripId(trip.tripId)
    setNotice('')
    setScreen(trip.chilled && trip.status !== 'LOADED' ? 'fridge' : 'loading')
  }
  const back = () => {
    setTripId(null)
    setShortfallStop(null)
    setNotice('')
    setScreen('loading')
  }

  return (
    <LoaderLayout
      context={
        detail.data && screen !== 'complete'
          ? `${detail.data.trip.vehicleId} · Trip ${detail.data.trip.tripNo} · ${detail.data.trip.district}`
          : undefined
      }
    >
      {tripId && (
        <button className="mb-5 min-h-12 font-semibold text-brand" onClick={back}>
          ← Back to trips
        </button>
      )}
      {!tripId && (
        <Trips
          trips={trips.data?.items ?? []}
          availableAt={trips.data?.listsAvailableAt}
          loading={trips.isLoading}
          error={trips.error}
          onSelect={selectTrip}
        />
      )}
      {tripId && detail.isLoading && <p className="text-muted">Loading trip…</p>}
      {tripId && detail.error && (
        <p role="alert" className="rounded-xl bg-danger-soft p-4 text-danger">
          {errorText(detail.error)}
        </p>
      )}
      {tripId && detail.data && (
        <>
          {screen === 'fridge' && (
            <Fridge
              trip={detail.data}
              onContinue={() => setScreen('loading')}
              onBack={back}
              onRefresh={refresh}
            />
          )}
          {screen === 'loading' && (
            <Loading
              trip={detail.data}
              notice={notice}
              onNotice={setNotice}
              onRefresh={refresh}
              onShortfall={setShortfallStop}
              onHandover={() => setScreen('handover')}
            />
          )}
          {screen === 'handover' && (
            <Handover
              trip={detail.data}
              onBack={() => setScreen('loading')}
              onDone={async () => {
                await refresh()
                setScreen('complete')
              }}
            />
          )}
          {screen === 'complete' && (
            <Completed
              trip={detail.data}
              next={trips.data?.items.find(
                (t) => t.tripId !== tripId && t.status !== 'LOADED' && t.status !== 'DEPARTED',
              )}
              onSelect={selectTrip}
              onBack={back}
            />
          )}
        </>
      )}
      {shortfallStop && (
        <Shortfall
          stop={shortfallStop}
          onClose={() => setShortfallStop(null)}
          onDone={async (ref) => {
            setShortfallStop(null)
            setNotice(
              `Report sent. Remainder order ${ref} was booked; the store and dispatcher were notified.`,
            )
            await refresh()
          }}
        />
      )}
    </LoaderLayout>
  )
}

function Trips({
  trips,
  availableAt,
  loading,
  error,
  onSelect,
}: {
  trips: TripSummary[]
  availableAt?: string
  loading: boolean
  error: unknown
  onSelect: (trip: TripSummary) => void
}) {
  if (loading) return <p className="text-muted">Loading trips…</p>
  if (error)
    return (
      <p role="alert" className="rounded-xl bg-danger-soft p-4 text-danger">
        {errorText(error)}
      </p>
    )
  if (!trips.length)
    return (
      <div className="mx-auto mt-14 max-w-2xl rounded-2xl border border-line bg-surface p-7 text-center sm:p-12">
        <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-full bg-brand-soft text-4xl text-brand">
          ◷
        </div>
        <h1 className="text-2xl font-bold">Loading lists aren’t ready yet</h1>
        <p className="mt-4 text-muted">
          The published loading lists appear at {availableAt ? time(availableAt) : '3:30 AM'}. Check
          again shortly.
        </p>
      </div>
    )
  return (
    <section>
      <h1 className="mb-4 text-2xl font-bold">Trips to load — next departures first</h1>
      <div className="space-y-3">
        {trips.map((trip, index) => (
          <button
            key={trip.tripId}
            onClick={() => onSelect(trip)}
            disabled={!trip.vehicleAvailable}
            className={`flex min-h-32 w-full items-center gap-4 rounded-xl border bg-surface p-4 text-left sm:p-6 ${index === 0 && trip.status === 'WAITING' ? 'border-2 border-brand' : 'border-line'} ${!trip.vehicleAvailable ? 'opacity-70' : ''}`}
          >
            <span className="hidden text-3xl text-brand sm:block">♧</span>
            <span className="min-w-0 flex-1">
              <strong className="block text-xl">
                {trip.vehicleId} · Trip {trip.tripNo}
              </strong>
              <span className="mt-1 block text-muted">
                {trip.district} · {trip.brand} · {trip.stops} stops · {trip.units} cases
              </span>
              {trip.chilled && (
                <span className="mt-2 inline-block rounded-full bg-status-chilled-soft px-3 py-1 text-sm font-semibold text-status-chilled">
                  ❄ Chilled
                </span>
              )}
            </span>
            <span className="text-right">
              <strong className="block whitespace-nowrap">Departs {time(trip.departAt)}</strong>
              <span className="mt-2 inline-block rounded-full bg-brand-soft px-3 py-1 text-sm font-semibold text-brand">
                {!trip.vehicleAvailable
                  ? '○ Later · vehicle in use'
                  : trip.status === 'WAITING'
                    ? '○ Waiting'
                    : trip.status === 'LOADING'
                      ? '◐ Loading'
                      : trip.status === 'READY'
                        ? '✓ Ready'
                        : trip.status === 'LOADED'
                          ? '✓ Handed over'
                          : '✓ Departed'}
              </span>
            </span>
            <span className="text-xl text-brand">›</span>
          </button>
        ))}
      </div>
    </section>
  )
}

function Fridge({
  trip,
  onContinue,
  onBack,
  onRefresh,
}: {
  trip: TripDetail
  onContinue: () => void
  onBack: () => void
  onRefresh: () => Promise<void>
}) {
  const [running, setRunning] = useState(true)
  const [temp, setTemp] = useState(3)
  const [doorsOk, setDoorsOk] = useState(true)
  const [editing, setEditing] = useState(!trip.fridgeCheck)
  const check = useMutation({
    mutationFn: () => loaderApi.fridgeCheck(trip.trip.tripId, { running, tempC: temp, doorsOk }),
    onSuccess: async () => {
      setEditing(false)
      await onRefresh()
    },
  })
  if (!editing && trip.fridgeCheck?.passed)
    return (
      <div className="mx-auto max-w-2xl rounded-2xl border border-line bg-surface p-6 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-status-delivered-soft text-3xl text-status-delivered">
          ✓
        </div>
        <h1 className="text-2xl font-bold">Fridge check passed</h1>
        <p className="my-4 text-muted">
          {trip.fridgeCheck.tempC} °C · checked {time(trip.fridgeCheck.checkedAt)}. Chilled goods
          can be loaded.
        </p>
        <button className={primary} onClick={onContinue}>
          Start loading
        </button>
      </div>
    )
  if (!editing && trip.fridgeCheck && !trip.fridgeCheck.passed)
    return (
      <>
        <div
          role="alert"
          className="rounded-xl border border-danger bg-danger-soft p-5 text-danger"
        >
          <h1 className="text-xl font-bold">Fridge check failed — do not load chilled goods</h1>
          <p className="mt-2">
            {trip.fridgeCheck.tempC} °C was recorded. The unit must be running at 0–5 °C with the
            doors closing properly. Dispatch was alerted.
          </p>
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-[2fr_1fr]">
          <div className="rounded-xl border border-line bg-surface p-6">
            <h2 className="text-xl font-bold">What happens now</h2>
            <p className="my-4 text-muted">
              Keep chilled cases in the cold room. Re-check after the fridge problem is fixed.
            </p>
            <div className="flex flex-wrap gap-3">
              <button className={secondary} onClick={() => setEditing(true)}>
                ↻ Re-check temperature
              </button>
              <button className={`${button} border border-line`} onClick={onBack}>
                Back to trips
              </button>
            </div>
          </div>
          <div className="rounded-xl border border-line bg-surface p-6">
            <h2 className="font-bold">Reading</h2>
            <p className="mt-3">Unit running: {trip.fridgeCheck.running ? 'Yes' : 'No'}</p>
            <p>Temperature: {trip.fridgeCheck.tempC} °C</p>
            <p>Doors OK: {trip.fridgeCheck.doorsOk ? 'Yes' : 'No'}</p>
          </div>
        </div>
      </>
    )
  return (
    <div className="grid gap-5 md:grid-cols-[2fr_1fr]">
      <section className="rounded-xl border border-line bg-surface p-5 sm:p-7">
        <h1 className="text-2xl font-bold">Fridge check before loading chilled goods</h1>
        <div className="mt-6 space-y-6">
          <div>
            <h2 className="font-bold">1 · Is the fridge unit running?</h2>
            <div className="mt-3 flex gap-3">
              <button className={running ? primary : secondary} onClick={() => setRunning(true)}>
                ✓ Yes, running
              </button>
              <button className={!running ? primary : secondary} onClick={() => setRunning(false)}>
                No
              </button>
            </div>
          </div>
          <div>
            <h2 className="font-bold">2 · Temperature on the unit display</h2>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <button
                className={`${secondary} min-w-14`}
                aria-label="Decrease temperature"
                onClick={() => setTemp((value) => Math.max(-99, value - 1))}
              >
                −
              </button>
              <input
                type="number"
                aria-label="Fridge temperature in Celsius"
                value={temp}
                onChange={(event) => setTemp(Number(event.target.value))}
                className="h-14 w-24 rounded-xl border border-line bg-surface text-center text-xl font-bold"
              />
              <span className="font-bold">°C</span>
              <button
                className={`${secondary} min-w-14`}
                aria-label="Increase temperature"
                onClick={() => setTemp((value) => Math.min(99, value + 1))}
              >
                +
              </button>
              <span className="text-sm text-muted">Required: 0–5 °C</span>
            </div>
          </div>
          <div>
            <h2 className="font-bold">3 · Doors and seals clean and closing properly?</h2>
            <div className="mt-3 flex gap-3">
              <button className={doorsOk ? primary : secondary} onClick={() => setDoorsOk(true)}>
                ✓ Yes
              </button>
              <button className={!doorsOk ? primary : secondary} onClick={() => setDoorsOk(false)}>
                No
              </button>
            </div>
          </div>
        </div>
        {check.error && (
          <p role="alert" className="mt-5 text-danger">
            {errorText(check.error)}
          </p>
        )}
        <div className="mt-7 flex flex-wrap gap-3">
          <button className={primary} disabled={check.isPending} onClick={() => check.mutate()}>
            {running && doorsOk && temp >= 0 && temp <= 5
              ? '✓ Start loading'
              : '⚠ Report fridge problem'}
          </button>
        </div>
      </section>
      <aside className="h-fit rounded-xl border border-line bg-surface p-6">
        <h2 className="text-xl font-bold">Why this check</h2>
        <p className="mt-4 text-muted">
          Chilled goods must stay at 0–5 °C. The reading is recorded under your name and shown at
          handover.
        </p>
      </aside>
    </div>
  )
}

function Loading({
  trip,
  notice,
  onNotice,
  onRefresh,
  onShortfall,
  onHandover,
}: {
  trip: TripDetail
  notice: string
  onNotice: (message: string) => void
  onRefresh: () => Promise<void>
  onShortfall: (stop: StopDetail) => void
  onHandover: () => void
}) {
  const [selected, setSelected] = useState<StopDetail | null>(null)
  const tick = useMutation({
    mutationFn: loaderApi.tick,
    onSuccess: async () => {
      setSelected(null)
      await onRefresh()
    },
  })
  const loaded = trip.stops.filter((stop) => stop.ticked).length
  const ready =
    trip.trip.status === 'READY' &&
    trip.loadedWeightKg <= trip.weightCapKg &&
    trip.loadedVolumeM3 <= trip.volumeCapM3
  return (
    <>
      {notice && (
        <p
          role="status"
          className="mb-5 rounded-xl border border-status-delivered bg-status-delivered-soft p-4 font-semibold text-status-delivered"
        >
          ✓ {notice}
        </p>
      )}
      <div className="grid gap-5 md:grid-cols-[2fr_1fr]">
        <section>
          <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
            <h1 className="text-2xl font-bold">Load in this order</h1>
            <span className="rounded-full bg-brand-soft px-3 py-1 font-semibold text-brand">
              {loaded} of {trip.stops.length} loaded
            </span>
          </div>
          <p className="mb-4 text-muted">
            Last stop goes in first, so stop 1 is at the door. Tap a row for handling details.
          </p>
          <div className="space-y-3">
            {trip.stops.map((stop, index) => (
              <button
                key={stop.stopId}
                onClick={() => setSelected(stop)}
                className={`flex min-h-28 w-full items-center gap-3 rounded-xl border p-4 text-left sm:gap-6 ${stop.ticked ? 'border-status-delivered bg-status-delivered-soft' : 'border-line bg-surface'}`}
              >
                <span className="text-center">
                  <span className="block text-xs font-bold uppercase text-muted">Load</span>
                  <strong className="text-3xl">{index + 1}</strong>
                </span>
                <span className="min-w-0 flex-1">
                  <strong className="block text-xl">{stop.outletId}</strong>
                  <span className="block text-sm text-muted">{stop.accessNote}</span>
                  <span className="block text-xs text-muted">
                    {stop.orderRef} · ≈ {Math.round(stop.weightKg)} kg · {stop.volumeM3.toFixed(1)}{' '}
                    m³
                  </span>
                </span>
                <span className="text-right">
                  <strong className="block text-lg">{stop.units} cases</strong>
                  {stop.missingUnits > 0 && (
                    <span className="block text-xs text-status-risk">
                      ⚠ {stop.missingUnits} short
                    </span>
                  )}
                </span>
                <span className="text-2xl text-status-delivered">{stop.ticked ? '☑' : '□'}</span>
              </button>
            ))}
          </div>
          {tick.error && (
            <p role="alert" className="mt-4 text-danger">
              {errorText(tick.error)}
            </p>
          )}
        </section>
        <aside className="space-y-3">
          <div className="rounded-xl border border-line bg-surface p-5">
            <h2 className="font-bold">
              {trip.trip.vehicleId} · {trip.vehicleType}
            </h2>
            <p className="mt-2 text-sm text-muted">Departs {time(trip.trip.departAt)}</p>
            <Capacity label="Weight" value={trip.loadedWeightKg} cap={trip.weightCapKg} unit="kg" />
            <Capacity label="Volume" value={trip.loadedVolumeM3} cap={trip.volumeCapM3} unit="m³" />
          </div>
          <button
            className={`${secondary} w-full`}
            onClick={() => {
              const stop = trip.stops.find((s) => !s.missingUnits && s.units > 1)
              if (stop) onShortfall(stop)
              else onNotice('Contact dispatch if an entire stop is short.')
            }}
          >
            ⚠ Flag missing or damaged
          </button>
          <button
            className={`${ready ? primary : button + ' bg-surface-2 text-muted'} w-full`}
            disabled={!ready}
            onClick={onHandover}
          >
            {ready ? '✓ Ready to depart' : `Ready to depart (${trip.stops.length - loaded} left)`}
          </button>
        </aside>
      </div>
      {selected && (
        <StopDrawer
          stop={selected}
          index={trip.stops.findIndex((s) => s.stopId === selected.stopId) + 1}
          pending={tick.isPending}
          onClose={() => setSelected(null)}
          onTick={() => tick.mutate(selected.stopId)}
          onShortfall={() => {
            setSelected(null)
            onShortfall(selected)
          }}
        />
      )}
    </>
  )
}

function Capacity({
  label,
  value,
  cap,
  unit,
}: {
  label: string
  value: number
  cap: number
  unit: string
}) {
  return (
    <div className="mt-4">
      <div className="flex justify-between text-sm">
        <span>{label}</span>
        <strong>
          {Math.round(value * 10) / 10} / {cap} {unit}
        </strong>
      </div>
      <div className="mt-1 h-2 rounded-full bg-surface-2">
        <div
          className={`h-2 rounded-full ${value > cap ? 'bg-danger' : 'bg-brand'}`}
          style={{ width: `${Math.min(100, cap ? (value / cap) * 100 : 0)}%` }}
        />
      </div>
    </div>
  )
}

function StopDrawer({
  stop,
  index,
  pending,
  onClose,
  onTick,
  onShortfall,
}: {
  stop: StopDetail
  index: number
  pending: boolean
  onClose: () => void
  onTick: () => void
  onShortfall: () => void
}) {
  return (
    <div className="fixed inset-0 z-30 bg-black/40" onClick={onClose}>
      <section
        role="dialog"
        aria-modal="true"
        aria-label={`Stop ${stop.outletId}`}
        className="absolute inset-y-0 right-0 flex w-full max-w-md flex-col bg-surface p-6 shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex justify-between">
          <h2 className="text-2xl font-bold">{stop.outletId}</h2>
          <button
            className="min-h-12 min-w-12 text-2xl"
            aria-label="Close stop details"
            onClick={onClose}
          >
            ×
          </button>
        </div>
        <p className="mt-2 text-muted">
          Load {index} · {stop.orderRef}
        </p>
        <div className="mt-5 rounded-xl bg-surface-2 p-4">
          <p>
            Cases <strong className="float-right">{stop.units}</strong>
          </p>
          <p className="mt-3">
            Weight <strong className="float-right">≈ {Math.round(stop.weightKg)} kg</strong>
          </p>
          <p className="mt-3">
            Volume <strong className="float-right">{stop.volumeM3.toFixed(1)} m³</strong>
          </p>
        </div>
        <h3 className="mt-6 text-sm font-bold uppercase text-muted">Handling notes</h3>
        <p className="mt-3">▣ {stop.accessNote}</p>
        {stop.storeNote && <p className="mt-3">⚠ Store note: {stop.storeNote}</p>}
        <div className="mt-auto space-y-3">
          <button
            className={`${primary} w-full`}
            disabled={pending || stop.ticked}
            onClick={onTick}
          >
            {stop.ticked ? '✓ Loaded' : `✓ Mark ${stop.units} cases loaded`}
          </button>
          <button
            className={`${secondary} w-full`}
            disabled={stop.missingUnits > 0 || stop.units <= 1}
            onClick={onShortfall}
          >
            ⚠ Flag missing or damaged
          </button>
        </div>
      </section>
    </div>
  )
}

function Shortfall({
  stop,
  onClose,
  onDone,
}: {
  stop: StopDetail
  onClose: () => void
  onDone: (ref: string) => Promise<void>
}) {
  const [units, setUnits] = useState(1)
  const [reason, setReason] = useState('MISSING')
  const [note, setNote] = useState('')
  const mutation = useMutation({
    mutationFn: () => loaderApi.shortfall(stop.stopId, { missingUnits: units, reason, note }),
    onSuccess: (data) => onDone(data.remainderOrderRef),
  })
  const send = (event: FormEvent) => {
    event.preventDefault()
    mutation.mutate()
  }
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center overflow-y-auto bg-black/50 p-3">
      <form
        role="dialog"
        aria-modal="true"
        aria-label="Flag shortfall"
        onSubmit={send}
        className="w-full max-w-lg rounded-2xl bg-surface p-5 shadow-xl sm:p-7"
      >
        <h2 className="text-2xl font-bold">Flag missing or damaged</h2>
        <p className="mt-2 text-muted">
          {stop.outletId} · {stop.orderRef} · {stop.units} cases
        </p>
        <div className="mt-5 flex items-center justify-between">
          <label htmlFor="short-units" className="font-semibold">
            Cases short
          </label>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className={`${secondary} min-w-14`}
              onClick={() => setUnits(Math.max(1, units - 1))}
            >
              −
            </button>
            <input
              id="short-units"
              type="number"
              min={1}
              max={stop.units - 1}
              value={units}
              onChange={(event) => setUnits(Number(event.target.value))}
              className="h-14 w-16 text-center text-2xl font-bold"
            />
            <button
              type="button"
              className={`${secondary} min-w-14`}
              onClick={() => setUnits(Math.min(stop.units - 1, units + 1))}
            >
              +
            </button>
          </div>
        </div>
        <p className="mt-5 font-semibold">Reason</p>
        <div className="mt-3 flex flex-wrap gap-2">
          {[
            ['MISSING', 'Missing from stock'],
            ['DAMAGED', 'Damaged'],
            ['WRONG_ITEM', 'Wrong item'],
          ].map(([value, label]) => (
            <button
              key={value}
              type="button"
              className={reason === value ? primary : secondary}
              onClick={() => setReason(value)}
            >
              {reason === value ? '✓ ' : ''}
              {label}
            </button>
          ))}
        </div>
        <label htmlFor="short-note" className="mt-5 block font-semibold">
          Note (optional)
        </label>
        <textarea
          id="short-note"
          value={note}
          onChange={(event) => setNote(event.target.value)}
          className="mt-2 min-h-20 w-full rounded-xl border border-line p-3"
        />
        <p className="mt-4 rounded-xl bg-brand-soft p-4 text-sm text-brand">
          Sending {Math.max(0, stop.units - units)} of {stop.units} cases. The {units} missing cases
          become a remainder order. The store and dispatcher are notified.
        </p>
        {mutation.error && (
          <p role="alert" className="mt-3 text-danger">
            {errorText(mutation.error)}
          </p>
        )}
        <div className="mt-5 grid grid-cols-2 gap-3">
          <button type="button" className={secondary} onClick={onClose}>
            Cancel
          </button>
          <button
            type="submit"
            className={primary}
            disabled={mutation.isPending || units < 1 || units >= stop.units}
          >
            ✓ Send report
          </button>
        </div>
      </form>
    </div>
  )
}

function Handover({
  trip,
  onBack,
  onDone,
}: {
  trip: TripDetail
  onBack: () => void
  onDone: () => Promise<void>
}) {
  const [staffId, setStaffId] = useState('')
  const mutation = useMutation({
    mutationFn: () => loaderApi.handover(trip.trip.tripId, staffId),
    onSuccess: onDone,
  })
  const submit = (event: FormEvent) => {
    event.preventDefault()
    mutation.mutate()
  }
  return (
    <div className="grid gap-5 md:grid-cols-[2fr_1fr]">
      <section className="rounded-xl border border-line bg-surface p-5 sm:p-7">
        <h1 className="text-2xl font-bold">Hand over to the driver</h1>
        <p className="mt-3 text-muted">
          The driver checks the cases against this list and confirms with their staff ID. After
          this, the load is the driver’s responsibility.
        </p>
        <div className="mt-5 space-y-3">
          {trip.stops.map((stop, index) => (
            <div
              key={stop.stopId}
              className="flex min-h-20 items-center gap-3 rounded-xl border border-line p-4"
            >
              <strong className="text-2xl text-brand">{index + 1}</strong>
              <span className="flex-1">
                <strong>{stop.outletId}</strong>
                <span className="block text-sm text-muted">
                  {stop.missingUnits ? `${stop.missingUnits} short · remainder booked` : 'Complete'}
                </span>
              </span>
              <strong>{stop.units} cases ✓</strong>
            </div>
          ))}
        </div>
        <form onSubmit={submit} className="mt-5">
          <label htmlFor="driver-staff" className="font-semibold">
            Driver staff ID
          </label>
          <input
            id="driver-staff"
            value={staffId}
            onChange={(event) => setStaffId(event.target.value)}
            autoComplete="off"
            className="mt-2 h-14 w-full rounded-xl border border-line px-4"
            placeholder="Enter the assigned driver’s staff ID"
            required
          />
          {mutation.error && (
            <p role="alert" className="mt-3 text-danger">
              {errorText(mutation.error)}
            </p>
          )}
          <div className="mt-4 flex flex-wrap gap-3">
            <button className={primary} disabled={mutation.isPending || !staffId.trim()}>
              ✓ Confirm handover
            </button>
            <button type="button" className={`${button} border border-line`} onClick={onBack}>
              Back to loading list
            </button>
          </div>
        </form>
      </section>
      <aside className="h-fit rounded-xl border border-line bg-surface p-5">
        <h2 className="text-xl font-bold">Trip summary</h2>
        <p className="mt-4">
          Cases{' '}
          <strong className="float-right">
            {trip.stops.reduce((sum, stop) => sum + stop.units, 0)}
          </strong>
        </p>
        <p className="mt-3">
          Weight{' '}
          <strong className="float-right">
            {Math.round(trip.loadedWeightKg)} / {trip.weightCapKg} kg
          </strong>
        </p>
        {trip.fridgeCheck && (
          <p className="mt-3">
            Fridge{' '}
            <strong className="float-right text-status-delivered">
              {trip.fridgeCheck.tempC} °C
            </strong>
          </p>
        )}
        <p className="mt-3">
          Departs <strong className="float-right">{time(trip.trip.departAt)}</strong>
        </p>
      </aside>
    </div>
  )
}

function Completed({
  trip,
  next,
  onSelect,
  onBack,
}: {
  trip: TripDetail
  next?: TripSummary
  onSelect: (trip: TripSummary) => void
  onBack: () => void
}) {
  return (
    <div className="mx-auto max-w-2xl rounded-2xl border border-line bg-surface p-7 text-center sm:p-10">
      <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-full bg-status-delivered-soft text-4xl text-status-delivered">
        ✓
      </div>
      <h1 className="text-2xl font-bold">
        {trip.trip.vehicleId} trip {trip.trip.tripNo} handed over
      </h1>
      <p className="mt-3 text-muted">
        {trip.stops.reduce((sum, stop) => sum + stop.units, 0)} cases · the driver can now start the
        route.
      </p>
      {next && (
        <div className="mt-6 flex flex-wrap items-center justify-between gap-4 rounded-xl bg-brand-soft p-5 text-left">
          <div>
            <strong>
              Next: {next.vehicleId} · Trip {next.tripNo} · {next.district}
            </strong>
            <p className="text-sm text-muted">
              Departs {time(next.departAt)} · {next.units} cases
            </p>
          </div>
          <button className={primary} onClick={() => onSelect(next)}>
            Load now
          </button>
        </div>
      )}
      <button className={`${secondary} mt-6`} onClick={onBack}>
        Back to trips
      </button>
    </div>
  )
}
