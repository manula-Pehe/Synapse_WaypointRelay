import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useParams } from 'react-router-dom'
import { enqueue, startSyncRunner, useSyncStatus } from '../../lib/offline'
import { mockTrip } from './mocks'
import { useDriverTheme } from './theme'
import DriverThemeProvider from './ThemeProvider'
import type { Language } from './i18n'
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
import type { DriverTrip, DriverStop } from './types'

/**
 * The driver app (F2 to F11).
 *
 * The run is held in memory for now and reads from the offline cache once `GET /api/driver/today`
 * lands; nothing else about the screens changes when it does. Every write goes through the outbox,
 * so a screen is usable with no signal.
 */
export default function DriverApp() {
  useEffect(() => {
    startSyncRunner()
  }, [])

  return (
    <BrowserRouter>
      <DriverThemeProvider>
        <Routes>
          <Route path="/driver" element={<Navigate to="/driver/sign-in" replace />} />
          <Route path="/driver/*" element={<DriverRoutes />} />
          <Route path="*" element={<Navigate to="/driver/sign-in" replace />} />
        </Routes>
      </DriverThemeProvider>
    </BrowserRouter>
  )
}

type UpdateStop = (id: string, patch: Partial<DriverStop>) => void

function DriverRoutes() {
  const { toggle } = useDriverTheme()
  const [language, setLanguage] = useState<Language>('en')
  const { state } = useSyncStatus()

  // One trip for the demo. `updateStop` is the seam where real data replaces this.
  const [trip, setTrip] = useState<DriverTrip>(mockTrip)

  const byId = useMemo(() => new Map(trip.stops.map((stop) => [stop.id, stop])), [trip])

  const updateStop: UpdateStop = (id, patch) => {
    setTrip((current) => ({
      ...current,
      stops: current.stops.map((stop) => (stop.id === id ? { ...stop, ...patch } : stop)),
    }))
  }

  const shared = { language, onToggleTheme: toggle }

  return (
    <Routes>
      <Route path="sign-in" element={<SignIn {...shared} offline={state === 'offline'} />} />

      <Route path="today" element={<Today {...shared} trip={trip} />} />

      <Route
        path="accept"
        element={
          <AcceptLoad
            {...shared}
            trip={trip}
            onAccepted={() => setTrip((current) => ({ ...current, loadAccepted: true }))}
          />
        }
      />

      <Route path="stops" element={<Stops {...shared} trip={trip} />} />

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
      <Route path="problem" element={<VehicleProblem {...shared} trip={trip} reply={null} />} />
      <Route path="trip-end" element={<TripEnd {...shared} trip={trip} />} />
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