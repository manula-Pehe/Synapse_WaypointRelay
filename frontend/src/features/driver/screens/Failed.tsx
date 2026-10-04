import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { enqueue } from '../../../lib/offline'
import DriverLayout from '../DriverLayout'
import { Button, Card, Label, TOUCH, Value } from '../components'
import { t, type Language } from '../i18n'
import { useDriverTheme } from '../theme'
import type { DriverStop } from '../types'

export interface FailedProps {
  stop: DriverStop
  language: Language
  onToggleTheme: () => void
  onRecorded: () => void
}

const REASONS = ['STORE_CLOSED', 'NO_ACCESS', 'REFUSED', 'DAMAGED'] as const

/**
 * why a delivery did not go through.
 *
 * The driver picks a reason instead of typing, because the record is the proof and a typed
 * explanation is the one thing that cannot be reconciled later. The server turns a short delivery
 * into a remainder order so the store still gets the rest.
 */
export default function Failed({ stop, language, onToggleTheme, onRecorded }: FailedProps) {
  const [nothingDelivered, setNothingDelivered] = useState(false)
  const { colors } = useDriverTheme()
  const [reason, setReason] = useState<(typeof REASONS)[number] | null>(null)
  const [units, setUnits] = useState(nothingDelivered ? 0 : stop.cases)
  const navigate = useNavigate()

  const shortBy = stop.cases - units

  async function record() {
    await enqueue('DELIVERY_RECORDED', {
      stopId: stop.id,
      orderId: stop.orderRef,
      outcome: nothingDelivered ? 'FAILED' : 'PARTIAL',
      units,
      ...(reason ? { reason } : {}),
      completedAt: new Date().toISOString(),
    })
    onRecorded()
    navigate('/driver/stops')
  }

  return (
    <DriverLayout
      title={t(language, 'driver.failed.title')}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => navigate(`/driver/stop/${stop.id}`)}
      action={
        <Button full onClick={record} disabled={reason === null} testId="record-failed">
          {t(language, 'driver.delivery.deliver')}
        </Button>
      }
    >
      <Card className="space-y-3">
        <Choice
          selected={!nothingDelivered}
          onSelect={() => {
            setNothingDelivered(false)
            setUnits(stop.cases)
          }}
        >
          {t(language, 'driver.failed.short')}
        </Choice>
        <Choice selected={nothingDelivered} onSelect={() => { setNothingDelivered(true); setUnits(0) }}>
          {t(language, 'driver.failed.none')}
        </Choice>
      </Card>

      {!nothingDelivered && (
        <Card>
          <Label>{t(language, 'driver.failed.units')}</Label>
          <div className="mt-2 flex items-center gap-3">
            <Button
              variant="secondary"
              onClick={() => setUnits((value) => Math.max(0, value - 5))}
              testId="fewer"
            >
              −5
            </Button>
            <Value size="lg">{units}</Value>
            <Button
              variant="secondary"
              onClick={() => setUnits((value) => Math.min(stop.cases, value + 5))}
              testId="more"
            >
              +5
            </Button>
          </div>
          {shortBy > 0 && (
            <p className="mt-2 text-sm" style={{ color: colors.ink2 }}>
              {shortBy} will go on the next run.
            </p>
          )}
        </Card>
      )}

      <Card className="space-y-2">
        <Label>{t(language, 'driver.failed.title')}</Label>
        {REASONS.map((option) => (
          <Choice
            key={option}
            selected={reason === option}
            onSelect={() => setReason(option)}
            testId={`reason-${option}`}
          >
            {t(language, `driver.failed.reason.${camel(option)}` as Parameters<typeof t>[1])}
          </Choice>
        ))}
      </Card>
    </DriverLayout>
  )
}

function Choice({
  children,
  selected,
  onSelect,
  testId,
}: {
  children: React.ReactNode
  selected: boolean
  onSelect: () => void
  testId?: string
}) {
  const [hover, setHover] = useState(false)
  const { colors } = useDriverTheme()
  return (
    <button
      type="button"
      data-testid={testId}
      onClick={onSelect}
      onPointerEnter={() => setHover(true)}
      onPointerLeave={() => setHover(false)}
      className={`${TOUCH} w-full rounded-xl border px-4 text-left text-lg`}
      style={{
        background: selected ? colors.brand : hover ? colors.surface2 : colors.surface,
        borderColor: selected ? colors.brand : colors.border,
        color: selected ? colors.bg : colors.ink,
      }}
    >
      {children}
    </button>
  )
}

function camel(value: string) {
  return value.toLowerCase() as 'storeClosed' | 'noAccess' | 'refused' | 'damaged'
}