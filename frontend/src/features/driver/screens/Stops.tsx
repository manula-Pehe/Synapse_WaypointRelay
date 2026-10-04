import { useNavigate } from 'react-router-dom'
import DriverLayout from '../DriverLayout'
import { Badge, Button, Card } from '../components'
import { t, type Language } from '../i18n'
import { useDriverTheme } from '../theme'
import type { DriverTrip, DriverStop } from '../types'

export interface StopsProps {
  trip: DriverTrip
  language: Language
  onToggleTheme: () => void
}

/**
 * R2 - the stops in delivery order, and the screen the driver lives in.
 *
 * R2-light is the same screen after the sun/moon switch, because the theme is remembered rather
 * than being a separate screen. Everything comes from the offline cache, so it renders with no signal.
 */
export default function Stops({ trip, language, onToggleTheme }: StopsProps) {
  const navigate = useNavigate()
  const next = trip.stops.find((stop) => stop.status === 'PENDING' || stop.status === 'ARRIVED')

  return (
    <DriverLayout
      title={t(language, 'driver.stops.title')}
      language={language}
      onToggleTheme={onToggleTheme}
      action={
        next && (
          <Button full onClick={() => navigate(`/driver/stop/${next.id}`)} testId="to-next-stop">
            {next.outlet.id} · {next.cases} {t(language, 'driver.stops.cases')}
          </Button>
        )
      }
    >
      {!next && (
        <Card>
          <p>{t(language, 'driver.tripEnd.done')}</p>
        </Card>
      )}

      <ol className="space-y-2">
        {trip.stops.map((stop) => (
          <li key={stop.id}>
            <StopRow
              stop={stop}
              language={language}
              onOpen={() => navigate(`/driver/stop/${stop.id}`)}
            />
          </li>
        ))}
      </ol>
    </DriverLayout>
  )
}

function toneFor(stop: DriverStop) {
  if (stop.status === 'DONE') return 'ok' as const
  if (stop.status === 'FAILED') return 'danger' as const
  if (stop.status === 'ARRIVED') return 'brand' as const
  if (stop.status === 'DEFERRED') return 'warn' as const
  return 'neutral' as const
}

function StopRow({ stop, language, onOpen }: { stop: DriverStop; language: Language; onOpen: () => void }) {
  const settled = stop.status === 'DONE' || stop.status === 'FAILED' || stop.status === 'DEFERRED'
  const { colors } = useDriverTheme()

  return (
    <button
      type="button"
      onClick={onOpen}
      className="flex min-h-16 w-full items-center gap-3 rounded-2xl border p-3 text-left active:opacity-70"
      data-testid={`stop-${stop.id}`}
    >
      <span
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border text-lg"
        style={{ opacity: settled ? 0.5 : 1 }}
        aria-hidden
      >
        {stop.sequence}
      </span>

      <span className="min-w-0 flex-1">
        <span className="block truncate font-medium">
          {stop.outlet.id} · {stop.outlet.district}
        </span>
        <span className="block text-sm" style={{ color: colors.ink2 }}>
          {stop.cases} {t(language, 'driver.stops.cases')} ·{' '}
          {stop.earlyByMinutes > 0
            ? `${stop.earlyByMinutes} min early`
            : `${stop.predictedArrival}`}
        </span>
      </span>

      <Badge tone={toneFor(stop)}>
        {stop.status === 'DONE'
          ? t(language, 'driver.stops.done')
          : stop.status === 'ARRIVED'
            ? t(language, 'driver.stops.arrive')
            : `${stop.outlet.windowOpen}–${stop.outlet.windowClose}`}
      </Badge>
    </button>
  )
}