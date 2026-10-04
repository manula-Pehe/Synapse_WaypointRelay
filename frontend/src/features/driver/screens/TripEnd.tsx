import { useNavigate } from 'react-router-dom'
import { useSyncStatus } from '../../../lib/offline'
import DriverLayout from '../DriverLayout'
import { Badge, Button, Card, Label, Value } from '../components'
import { t, type Language } from '../i18n'
import { useDriverTheme } from '../theme'
import type { DriverTrip } from '../types'

export interface TripEndProps {
  trip: DriverTrip
  language: Language
  onToggleTheme: () => void
}

/**
 * R8 and R10 - the trip is complete, and a warning if the driver signs out with work still waiting.
 *
 * US-11.3 is the part that protects the offline story: leaving the depot with unsynced work is how
 * records get lost, so the screen says so plainly rather than letting the driver walk away.
 */
export default function TripEnd({ trip, language, onToggleTheme }: TripEndProps) {
  const navigate = useNavigate()
  const { waiting } = useSyncStatus()
  const { colors } = useDriverTheme()

  const delivered = trip.stops.filter((stop) => stop.status === 'DONE')
  const failed = trip.stops.filter((stop) => stop.status === 'FAILED')
  const cases = delivered.reduce((sum, stop) => sum + stop.cases, 0)

  return (
    <DriverLayout
      title={t(language, 'driver.tripEnd.title')}
      language={language}
      onToggleTheme={onToggleTheme}
      action={
        <Button full onClick={() => navigate('/driver/sign-in')} testId="finish">
          {t(language, 'driver.tripEnd.next')}
        </Button>
      }
    >
      <Card>
        <Label>{trip.no}</Label>
        <Value size="lg">
          {delivered.length} {t(language, 'driver.tripEnd.done')}
        </Value>
        <div className="mt-3 flex flex-wrap gap-2">
          <Badge tone="ok">{cases} {t(language, 'driver.stops.cases')}</Badge>
          {failed.length > 0 && <Badge tone="danger">{failed.length} {t(language, 'driver.failed.none')}</Badge>}
        </div>
      </Card>

      {waiting > 0 && (
        <Card style={{ borderColor: colors.warn }}>
          <p style={{ color: colors.warn }} data-testid="unsynced-warning">
            {t(language, 'driver.sync.waiting', { count: waiting })}
          </p>
        </Card>
      )}

      {/* R8r — cases still on the truck go back on the shelf before the driver signs out. */}
      <Button variant="secondary" full onClick={() => navigate('/driver/hand-back')} testId="to-hand-back">
        {t(language, 'driver.tripEnd.handback')}
      </Button>
    </DriverLayout>
  )
}