import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { enqueue } from '../../../lib/offline'
import DriverLayout from '../DriverLayout'
import { Button, Card, Label, Value } from '../components'
import { t, type Language } from '../i18n'
import type { DriverStop } from '../types'

export interface StoreWaitProps {
  stop: DriverStop
  language: Language
  onToggleTheme: () => void
  onRecorded: () => void
}

/**
 * R4w - the driver got there early and the shop is shut.
 *
 * the wait is recorded rather than the goods being sent back, because a store that opens
 * late is not a failed delivery. It becomes a STORE_WAIT in the outbox and stays on the record even
 * if the delivery then succeeds.
 */
export default function StoreWait({ stop, language, onToggleTheme, onRecorded }: StoreWaitProps) {
  const [startedAt, setStartedAt] = useState<string | null>(null)
  const [seconds, setSeconds] = useState(0)
  const navigate = useNavigate()

  useEffect(() => {
    if (startedAt === null) return
    const timer = window.setInterval(() => {
      setSeconds(Math.floor((Date.now() - Date.parse(startedAt)) / 1000))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [startedAt])

  async function finish() {
    const endedAt = new Date().toISOString()
    await enqueue('STORE_WAIT', {
      stopId: stop.id,
      startedAt,
      endedAt,
      note: 'Store closed on arrival',
    })
    onRecorded()
    navigate(`/driver/stop/${stop.id}`)
  }

  return (
    <DriverLayout
      title={t(language, 'driver.wait.title')}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => navigate(`/driver/stop/${stop.id}`)}
      action={
        startedAt === null ? (
          <Button full onClick={() => setStartedAt(new Date().toISOString())} testId="start-waiting">
            {t(language, 'driver.wait.start')}
          </Button>
        ) : (
          <Button full onClick={finish} testId="done-waiting">
            {t(language, 'driver.wait.end')}
          </Button>
        )
      }
    >
      <Card>
        <Label>{stop.outlet.id}</Label>
        <Value size="lg">
          {stop.outlet.windowOpen}–{stop.outlet.windowClose}
        </Value>
      </Card>

      {startedAt !== null && (
        <Card>
          <Label>{t(language, 'driver.wait.waiting')}</Label>
          <Value size="lg">
            {String(Math.floor(seconds / 60)).padStart(2, '0')}:
            {String(seconds % 60).padStart(2, '0')}
          </Value>
        </Card>
      )}
    </DriverLayout>
  )
}