import { useNavigate } from 'react-router-dom'
import DriverLayout from '../DriverLayout'
import { Badge, Button, Card, Label, Value } from '../components'
import { t, type Language } from '../i18n'
import { useDriverTheme } from '../theme'
import { ACCESS_NOTE, type DriverStop } from '../types'

export interface StopDetailProps {
  stop: DriverStop
  language: Language
  onToggleTheme: () => void
  onArrived: () => void
}

/**
 * R3 and R3a - stop details, and what to do when the driver gets there early.
 *
 * US-4.2 is the reason the early case is called out: unloading into a shut shop wastes the run, so
 * the screen says how long the wait will be and offers to record it (R4w).
 */
export default function StopDetail({ stop, language, onToggleTheme, onArrived }: StopDetailProps) {
  const navigate = useNavigate()
  const { colors } = useDriverTheme()
  const waiting = stop.earlyByMinutes > 0

  return (
    <DriverLayout
      title={`${stop.sequence}. ${stop.outlet.id}`}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => navigate('/driver/stops')}
      action={
        stop.status === 'ARRIVED' ? (
          <Button full onClick={() => navigate(`/driver/stop/${stop.id}/deliver`)} testId="to-deliver">
            {t(language, 'driver.delivery.deliver')}
          </Button>
        ) : (
          <Button full onClick={onArrived} testId="arrive">
            {t(language, 'driver.stops.arrive')}
          </Button>
        )
      }
    >
      <Card>
        <Label>{t(language, 'driver.stops.window')}</Label>
        <Value size="lg">
          {stop.outlet.windowOpen}–{stop.outlet.windowClose}
        </Value>
        <div className="mt-2 flex items-center gap-2">
          {stop.chilled && <Badge tone="brand">Chilled</Badge>}
          <Badge tone="neutral">
            {stop.cases} {t(language, 'driver.stops.cases')}
          </Badge>
        </div>
      </Card>

      {waiting && (
        <Card className="space-y-2">
          <Label>{t(language, 'driver.wait.title')}</Label>
          <p className="text-lg">
            Opens at {stop.outlet.windowOpen} — you are {stop.earlyByMinutes} min early.
          </p>
          <Button
            variant="secondary"
            full
            onClick={() => navigate(`/driver/stop/${stop.id}/wait`)}
            testId="record-wait"
          >
            {t(language, 'driver.wait.start')}
          </Button>
        </Card>
      )}

      <Card>
        <Label>{t(language, 'driver.stop.access')}</Label>
        <p className="mt-1">{ACCESS_NOTE[stop.outlet.dockType]}</p>
        {stop.outlet.note && (
          <p className="mt-1 text-sm" style={{ color: colors.ink2 }}>
            {stop.outlet.note}
          </p>
        )}
      </Card>

      <Card>
        <Label>{stop.orderRef}</Label>
        <Value>{stop.cases}</Value>
      </Card>

      <Button variant="ghost" full onClick={() => undefined}>
        {t(language, 'driver.stop.call')}
      </Button>
    </DriverLayout>
  )
}