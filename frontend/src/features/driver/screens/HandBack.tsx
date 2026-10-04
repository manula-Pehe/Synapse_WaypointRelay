import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { enqueue } from '../../../lib/offline'
import DriverLayout from '../DriverLayout'
import { Button, Card, Label, TOUCH, Value } from '../components'
import { t, type Language } from '../i18n'
import { outletLabel } from '../outlet'
import { useDriverTheme } from '../theme'
import type { DriverTrip } from '../types'

export interface HandBackProps {
  trip: DriverTrip
  language: Language
  onToggleTheme: () => void
}

const REASONS = ['STORE_CLOSED', 'NO_ACCESS', 'REFUSED', 'DAMAGED'] as const

type Reason = (typeof REASONS)[number]

/**
 * R8r - goods handed back at the depot at the end of the run.
 *
 * Stock that stayed on the truck goes back on the shelf, and this is the record of who handed it
 * back and why. It is queued like everything else in the driver app, so a driver who hands the load
 * back in a yard with no signal records it on the phone and it sends when there is one.
 *
 * The order keeps its own status: a handback is not a delivery, so the shortfall stays outstanding
 * and the store still gets the cases on a later run.
 */
export default function HandBack({ trip, language, onToggleTheme }: HandBackProps) {
  const navigate = useNavigate()
  const { colors } = useDriverTheme()

  // Only stops that never got the full load have cases on the truck to give back.
  const returning = trip.stops.filter(
    (stop) => stop.status === 'FAILED' || stop.status === 'DEFERRED' || stop.status === 'PENDING',
  )

  const [stopId, setStopId] = useState<string | null>(null)
  const [reason, setReason] = useState<Reason | null>(null)

  const stop = trip.stops.find((candidate) => candidate.id === stopId)
  const [units, setUnits] = useState(0)

  async function record() {
    if (!stop || !reason) return
    await enqueue('GOODS_RETURNED', {
      tripId: trip.id,
      orderId: stop.orderId ?? stop.orderRef,
      units,
      reason,
    })
    setStopId(null)
    setReason(null)
    setUnits(0)
    navigate('/driver/trip-end')
  }

  if (!stop) {
    return (
      <DriverLayout
        title={t(language, 'driver.handback.title')}
        language={language}
        onToggleTheme={onToggleTheme}
        onBack={() => navigate('/driver/trip-end')}
      >
        {returning.length === 0 ? (
          <Card>
            <p>{t(language, 'driver.handback.nothing')}</p>
          </Card>
        ) : (
          <ol className="space-y-2">
            {returning.map((candidate) => (
              <li key={candidate.id}>
                <Pick
                  selected={false}
                  onSelect={() => {
                    setStopId(candidate.id)
                    setUnits(candidate.cases)
                  }}
                  testId={`handback-${candidate.id}`}
                >
                  <span className="block">
                    {candidate.sequence}. {outletLabel(candidate.outlet)}
                  </span>
                  <span className="block text-sm" style={{ color: colors.ink2 }}>
                    {candidate.cases} {t(language, 'driver.stops.cases')}
                  </span>
                </Pick>
              </li>
            ))}
          </ol>
        )}
      </DriverLayout>
    )
  }

  return (
    <DriverLayout
      title={t(language, 'driver.handback.title')}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => setStopId(null)}
      action={
        <Button
          full
          onClick={record}
          disabled={reason === null || units < 1}
          testId="record-handback"
        >
          {t(language, 'driver.handback.record')}
        </Button>
      }
    >
      <Card>
        <Label>{outletLabel(stop.outlet)}</Label>
        <div className="mt-2 flex items-center gap-3">
          <Button
            variant="secondary"
            onClick={() => setUnits((value) => Math.max(0, value - 5))}
            testId="handback-fewer"
          >
            −5
          </Button>
          <Value size="lg">{units}</Value>
          <Button
            variant="secondary"
            onClick={() => setUnits((value) => Math.min(stop.cases, value + 5))}
            testId="handback-more"
          >
            +5
          </Button>
        </div>
        <p className="mt-2 text-sm" style={{ color: colors.ink2 }}>
          {t(language, 'driver.handback.hint')}
        </p>
      </Card>

      <Card className="space-y-2">
        <Label>{t(language, 'driver.handback.why')}</Label>
        {REASONS.map((option) => (
          <Pick
            key={option}
            selected={reason === option}
            onSelect={() => setReason(option)}
            testId={`handback-reason-${option}`}
          >
            {t(language, `driver.failed.reason.${camel(option)}` as Parameters<typeof t>[1])}
          </Pick>
        ))}
      </Card>
    </DriverLayout>
  )
}

function Pick({
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
