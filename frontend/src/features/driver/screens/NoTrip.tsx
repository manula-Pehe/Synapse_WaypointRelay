import DriverLayout from '../DriverLayout'
import { Button, Card, Label, Value } from '../components'
import { t, type Language } from '../i18n'
import { useDriverTheme } from '../theme'
import type { RunEmpty } from '../useRun'

export interface NoTripProps {
  /** Which of the three reasons there is no run. */
  reason: RunEmpty
  language: Language
  onToggleTheme: () => void
  onRetry: () => void
}

/**
 * What the driver sees when the server has no trip for this vehicle.
 *
 * The one rule here is that nothing on this screen is invented. Before the plan is published, or
 * when the phone cannot reach the server and has no run saved, the honest answer is that there is
 * nothing to drive yet - a made-up list of stops would be indistinguishable from a real one to
 * anyone reading the screen.
 */
export default function NoTrip({ reason, language, onToggleTheme, onRetry }: NoTripProps) {
  const { colors } = useDriverTheme()

  return (
    <DriverLayout
      title={t(language, 'driver.noTrip.title')}
      language={language}
      onToggleTheme={onToggleTheme}
      action={
        // Nothing to retry while the first fetch is still in flight.
        reason === 'loading' ? undefined : (
          <Button full onClick={onRetry} testId="retry-run">
            {t(language, 'driver.noTrip.retry')}
          </Button>
        )
      }
    >
      <Card>
        <Label>{t(language, reason === 'offline' ? 'driver.noTrip.offlineTitle' : 'driver.noTrip.waitingTitle')}</Label>
        <Value size="lg">
          {reason === 'loading'
            ? t(language, 'driver.noTrip.loading')
            : reason === 'offline'
              ? t(language, 'driver.noTrip.offline')
              : t(language, 'driver.noTrip.waiting')}
        </Value>
        <p className="mt-3 text-sm" style={{ color: colors.ink2 }}>
          {t(language, reason === 'offline' ? 'driver.noTrip.offlineHint' : 'driver.noTrip.waitingHint')}
        </p>
      </Card>
    </DriverLayout>
  )
}