import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { enqueue, startSyncRunner, useSyncStatus } from '../../lib/offline'
import { useDriverTheme } from './theme'
import DriverThemeProvider from './ThemeProvider'
import { useRun } from './useRun'
import { useDriverReply } from './api'
import { rememberLanguage, storedLanguage, type Language } from './i18n'
import SignIn from './screens/SignIn'
import Today from './screens/Today'
import AcceptLoad from './screens/AcceptLoad'
import Stops from './screens/Stops'
import StopDetail from './screens/StopDetail'
import Deliver from './screens/Deliver'
import Failed from './screens/Failed'
import StoreWait from './screens/StoreWait'
import SyncSummary from './screens/SyncSummary'
import Menu from './screens/Menu'
import VehicleProblem from './screens/VehicleProblem'
import TripEnd from './screens/TripEnd'
import HandBack from './screens/HandBack'
import type { DriverStop } from './types'

/**
 * The driver app (F2 to F11).
 *
 * The run comes from `useRun`: `GET /api/driver/today`, cached in Dexie and painted from there when
 * there is no signal. Every write goes through the outbox, so a screen is usable with no signal.
 *
 * This is mounted by the main router under `/driver`, so it brings no router of its own and its
 * routes are relative. Signing in is the app's job too - a driver reaches this through `/login` and
 * the `DRIVER` guard - so `sign-in` just hands anyone who arrives unauthenticated back to it.
 */
export default function DriverApp() {
  useEffect(() => {
    startSyncRunner()
  }, [])

  return (
    <DriverThemeProvider>
      <Routes>
        <Route index element={<Navigate to="sign-in" replace />} />
        <Route path="*" element={<DriverRoutes />} />
      </Routes>
    </DriverThemeProvider>
  )
}

type UpdateStop = (id: string, patch: Partial<DriverStop>) => void

function DriverRoutes() {
  const { toggle } = useDriverTheme()
  const [language, setLanguage] = useState<Language>(storedLanguage)
  const { state } = useSyncStatus()
  const { run, trip, acceptLoad, refresh } = useRun()
  const latestReply = useDriverReply()

  // Stops change on this phone as the driver works, so the run is kept alongside the server's copy
  // and merged back in on the next refresh.
  const [localStops, setLocalStops] = useState<DriverStop[] | null>(null)

  const stops = useMemo(() => localStops ?? trip.stops, [localStops, trip.stops])

  const view = useMemo(() => ({ ...trip, stops }), [trip, stops])
  const byId = useMemo(() => new Map(stops.map((stop) => [stop.id, stop])), [stops])

  const updateStop: UpdateStop = (id, patch) => {
    setLocalStops((current) => {
      const base = current ?? trip.stops
      return base.map((stop) => (stop.id === id ? { ...stop, ...patch } : stop))
    })
  }

  /** Drop the local overlay so the screen shows the server's run again. */
  const clearLocal = () => {
    setLocalStops(null)
    void refresh()
  }

  // F12 - the choice is remembered like the theme, and the document is told so that the Sinhala
  // and Tamil faces and their taller line height apply. Without this a Tamil driver reads a
  // fallback font clipped to the Latin line box.
  useEffect(() => {
    document.documentElement.lang = language
    rememberLanguage(language)
  }, [language])

  const shared = { language, onToggleTheme: toggle }

  return (
    <Routes>
      <Route path="sign-in" element={<SignIn {...shared} offline={state === 'offline'} />} />

      <Route path="today" element={<Today {...shared} trip={view} run={run} />} />

      <Route
        path="accept"
        element={
          <AcceptLoad
            {...shared}
            trip={view}
            onAccepted={async () => {
              await acceptLoad()
              clearLocal()
            }}
          />
        }
      />

      <Route path="stops" element={<Stops {...shared} trip={view} />} />

      <Route
        path="stop/:stopId"
        element={
          <StopRoute byId={byId}>
            {(stop) => (
              <StopDetail
                {...shared}
                stop={stop}
                onArrived={() => {
                  updateStop(stop.id, { status: 'ARRIVED' })
                  void enqueue('ARRIVED', { stopId: stop.id, arrivedAt: new Date().toISOString() })
                }}
              />
            )}
          </StopRoute>
        }
      />

      <Route
        path="stop/:stopId/deliver"
        element={
          <StopRoute byId={byId}>
            {(stop) => (
              <Deliver {...shared} stop={stop} onRecorded={() => updateStop(stop.id, { status: 'DONE' })} />
            )}
          </StopRoute>
        }
      />

      <Route
        path="stop/:stopId/failed"
        element={
          <StopRoute byId={byId}>
            {(stop) => (
              <Failed {...shared} stop={stop} onRecorded={() => updateStop(stop.id, { status: 'FAILED' })} />
            )}
          </StopRoute>
        }
      />

      <Route
        path="stop/:stopId/wait"
        element={
          <StopRoute byId={byId}>
            {(stop) => <StoreWait {...shared} stop={stop} onRecorded={() => undefined} />}
          </StopRoute>
        }
      />

      <Route path="sync" element={<SyncSummary {...shared} />} />
      <Route path="menu" element={<Menu {...shared} onLanguage={setLanguage} />} />
      <Route
        path="problem"
        element={
          // R9ok — dispatch's instruction, read from the server so the driver is not phoning.
          <VehicleProblem {...shared} trip={view} reply={latestReply} />
        }
      />
      <Route path="trip-end" element={<TripEnd {...shared} trip={view} />} />
      {/* R8r — goods that stayed on the truck go back on the shelf at the depot. */}
      <Route path="hand-back" element={<HandBack {...shared} trip={view} />} />
    </Routes>
  )
}


/** Renders the stop the URL names, or goes back to the list if this run does not have it. */
function StopRoute({
  byId,
  children,
}: {
  byId: Map<string, DriverStop>
  children: (stop: DriverStop) => ReactNode
}) {
  const { stopId } = useParams()
  const stop = stopId ? byId.get(stopId) : undefined

  if (!stop) return <Navigate to="/driver/stops" replace />
  return <>{children(stop)}</>
}