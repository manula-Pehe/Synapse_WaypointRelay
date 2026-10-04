import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import DriverLayout from '../DriverLayout'
import { Button, Card, Label, Value } from '../components'
import { t, type Language } from '../i18n'
import { outletLabel } from '../outlet'
import { useDriverTheme } from '../theme'
import type { DriverTrip } from '../types'

export interface AcceptLoadProps {
  trip: DriverTrip
  language: Language
  onToggleTheme: () => void
  onAccepted: () => void | Promise<void>
}

/**
 * R0 - the driver checks the load against the loader's list and accepts it.
 *
 * Acceptance moves responsibility to the driver, so it is a deliberate tap on a screen that shows
 * what is actually on the vehicle - cases per stop, any shortfall, and the fridge reading.
 */
export default function AcceptLoad({ trip, language, onToggleTheme, onAccepted }: AcceptLoadProps) {
  const [mismatch, setMismatch] = useState(false)
  const navigate = useNavigate()
  const { colors } = useDriverTheme()

  const onTheTruck = trip.stops.reduce((sum, stop) => sum + stop.cases, 0)

  async function accept() {
    if (onTheTruck !== trip.loadedCases) {
      setMismatch(true)
      return
    }
    // Awaited so the stops screen opens with the server's view rather than the pre-accept one.
    await onAccepted()
    navigate('/driver/stops')
  }

  return (
    <DriverLayout
      title={trip.no}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => navigate('/driver/today')}
      action={
        <Button full onClick={() => void accept()} testId="accept-load">
          {t(language, 'driver.today.accept')}
        </Button>
      }
    >
      <Card>
        <Label>{t(language, 'driver.today.vehicle')}</Label>
        <Value size="lg">{trip.vehicleId}</Value>
        {trip.fridgeTempC !== null && (
          <p className="mt-2 text-sm" style={{ color: colors.ink2 }}>
            {trip.fridgeTempC} °C
          </p>
        )}
      </Card>

      <Card>
        <Label>{t(language, 'driver.stops.cases')}</Label>
        <div className="mt-1 flex items-baseline gap-2">
          <Value size="lg">{onTheTruck}</Value>
          <span style={{ color: colors.ink2 }}>on the truck</span>
        </div>
        {mismatch && (
          <p className="mt-2 text-sm" style={{ color: colors.danger }} role="alert">
            The loader counted {trip.loadedCases}. Check with the loader before accepting.
          </p>
        )}
      </Card>

      <Card>
        <Label>{t(language, 'driver.stops.title')}</Label>
        <ul className="mt-2 space-y-2">
          {trip.stops.map((stop) => (
            <li key={stop.id} className="flex items-center justify-between gap-3">
              <span className="truncate">
                {stop.sequence}. {outletLabel(stop.outlet)}
              </span>
              <span style={{ color: colors.ink2 }}>
                {stop.cases} {t(language, 'driver.stops.cases')}
              </span>
            </li>
          ))}
        </ul>
      </Card>
    </DriverLayout>
  )
}