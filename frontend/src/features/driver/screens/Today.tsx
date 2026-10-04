import { useNavigate } from 'react-router-dom'
import DriverLayout from '../DriverLayout'
import { Badge, Button, Card, Label, Value } from '../components'
import { t, type Language } from '../i18n'
import { outletLabel } from '../outlet'
import { useDriverTheme } from '../theme'
import type { DriverRun, DriverTrip } from '../types'

export interface TodayProps {
  trip: DriverTrip
  /** Vehicle type and run date come from the run, not the trip. */
  run: DriverRun
  language: Language
  onToggleTheme: () => void
}

/** R1 — today's run at a glance, so the driver knows when and where without calling the depot. */
export default function Today({ trip, run, language, onToggleTheme }: TodayProps) {
  const navigate = useNavigate()
  const { colors } = useDriverTheme()
  const { done } = summarise(trip)

  return (
    <DriverLayout
      title={t(language, 'driver.today.title')}
      language={language}
      onToggleTheme={onToggleTheme}
      action={
        trip.loadAccepted ? (
          <Button full onClick={() => navigate('/driver/stops')} testId="to-stops">
            {t(language, 'driver.stops.title')}
          </Button>
        ) : (
          <Button full onClick={() => navigate('/driver/accept')} testId="to-accept">
            {t(language, 'driver.today.accept')}
          </Button>
        )
      }
    >
      <Card>
        <Label>{trip.no}</Label>
        <div className="mt-1 flex items-baseline gap-2">
          <Value size="lg">{done}</Value>
          <span style={{ color: colors.ink2 }}>
            / {trip.stops.length} {t(language, 'driver.today.stops')}
          </span>
        </div>
        <div className="mt-4 grid grid-cols-2 gap-3">
          <div>
            <Label>{t(language, 'driver.today.vehicle')}</Label>
            <Value>{run.vehicleType || trip.vehicleId}</Value>
          </div>
          <div>
            <Label>{t(language, 'driver.menu.depot')}</Label>
            <Value>{trip.depot}</Value>
          </div>
        </div>
      </Card>

      {trip.loadAccepted && (
        <Card>
          <Label>{t(language, 'driver.stop.detail')} 1</Label>
          <div className="mt-2 flex items-center justify-between gap-2">
            <div className="min-w-0">
              <div className="truncate font-medium">
                {trip.stops[0] && outletLabel(trip.stops[0].outlet)}
              </div>
              <div className="text-sm" style={{ color: colors.ink2 }}>
                {trip.stops[0]?.predictedArrival}
              </div>
            </div>
            <Badge tone="brand">{trip.stops[0]?.cases} {t(language, 'driver.stops.cases')}</Badge>
          </div>
        </Card>
      )}
    </DriverLayout>
  )
}

function summarise(trip: DriverTrip) {
  return {
    done: trip.stops.filter((stop) => stop.status === 'DONE' || stop.status === 'FAILED').length,
  }
}