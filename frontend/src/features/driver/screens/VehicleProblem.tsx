import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { enqueue } from '../../../lib/offline'
import DriverLayout from '../DriverLayout'
import { Button, Card, Label, TOUCH } from '../components'
import { t, type Language } from '../i18n'
import { useDriverTheme } from '../theme'
import type { DriverTrip } from '../types'

export interface VehicleProblemProps {
  trip: DriverTrip
  language: Language
  onToggleTheme: () => void
  /** Dispatch's written answer, shown on the same screen. */
  reply?: string | null
}

const KINDS = ['BREAKDOWN', 'FRIDGE_FAULT', 'ACCIDENT', 'TYRE', 'OTHER'] as const

/**
 * R9 - a vehicle problem in a few taps, with the fridge reading, how many cases are still on the
 * truck, and whether the driver can drive on, because that is what dispatch needs to decide about
 * the rest of the run.
 *
 * "Nothing on board" is a separate button from an empty box on purpose. Zero cases and nobody said
 * are different answers, and the breakdown re-plan moves the stranded stock.
 */
export default function VehicleProblem({ trip, language, onToggleTheme, reply }: VehicleProblemProps) {
  const [kind, setKind] = useState<(typeof KINDS)[number] | null>(null)
  const { colors } = useDriverTheme()
  const [canDrive, setCanDrive] = useState(true)
  const [fridge, setFridge] = useState('')
  const [onBoard, setOnBoard] = useState('')
  const [note, setNote] = useState('')
  const [sent, setSent] = useState(false)
  const navigate = useNavigate()

  async function send() {
    await enqueue('VEHICLE_PROBLEM', {
      tripId: trip.id,
      kind,
      canDrive,
      ...(fridge ? { fridgeTempC: Number(fridge) } : {}),
      ...(onBoard ? { unitsOnBoard: Number(onBoard) } : {}),
      ...(note ? { note } : {}),
    })
    setSent(true)
  }

  if (reply) {
    return (
      <DriverLayout
        title={t(language, 'driver.problem.title')}
        language={language}
        onToggleTheme={onToggleTheme}
        action={
          <Button full onClick={() => navigate('/driver/stops')}>
            {t(language, 'driver.stops.title')}
          </Button>
        }
      >
        <Card>
          <Label>{t(language, 'driver.problem.reply')}</Label>
          <p className="mt-2 text-xl">{reply}</p>
        </Card>
      </DriverLayout>
    )
  }

  if (sent) {
    return (
      <DriverLayout
        title={t(language, 'driver.problem.title')}
        language={language}
        onToggleTheme={onToggleTheme}
        action={
          <Button full onClick={() => navigate('/driver/stops')}>
            {t(language, 'driver.stops.title')}
          </Button>
        }
      >
        <Card>
          <p style={{ color: colors.ok }} data-testid="problem-sent">
            {t(language, 'driver.problem.sent')}
          </p>
        </Card>
      </DriverLayout>
    )
  }

  return (
    <DriverLayout
      title={t(language, 'driver.problem.title')}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => navigate('/driver/menu')}
      action={
        <Button full onClick={send} disabled={kind === null} testId="send-problem">
          {t(language, 'driver.problem.send')}
        </Button>
      }
    >
      <Card className="space-y-2">
        <Label>{t(language, 'driver.problem.title')}</Label>
        <div className="flex flex-wrap gap-2">
          {KINDS.map((option) => (
            <Button
              key={option}
              variant={kind === option ? 'primary' : 'secondary'}
              onClick={() => setKind(option)}
              testId={`kind-${option}`}
            >
              {option.replace('_', ' ')}
            </Button>
          ))}
        </div>
      </Card>

      <Card className="space-y-2">
        <Button
          variant={canDrive ? 'primary' : 'secondary'}
          onClick={() => setCanDrive(true)}
          testId="can-drive"
        >
          {t(language, 'driver.problem.canDrive')}
        </Button>
        <Button
          variant={canDrive ? 'secondary' : 'danger'}
          onClick={() => setCanDrive(false)}
          testId="cannot-drive"
        >
          {t(language, 'driver.problem.cannot')}
        </Button>
      </Card>

      <Card>
        <Label>{t(language, 'driver.problem.fridge')}</Label>
        <input
          className={`mt-2 w-full ${TOUCH} rounded-xl border px-4 text-xl`}
          style={{ background: colors.surface2, borderColor: colors.border, color: colors.ink }}
          inputMode="decimal"
          value={fridge}
          onChange={(event) => setFridge(event.target.value)}
          data-testid="fridge"
        />
      </Card>

      <Card>
        <Label>{t(language, 'driver.problem.onBoard')}</Label>
        <input
          className={`mt-2 w-full ${TOUCH} rounded-xl border px-4 text-xl`}
          style={{ background: colors.surface2, borderColor: colors.border, color: colors.ink }}
          inputMode="numeric"
          value={onBoard}
          onChange={(event) => setOnBoard(event.target.value.replace(/[^0-9]/g, ''))}
          data-testid="on-board"
        />
        <Button
          variant="secondary"
          onClick={() => setOnBoard('0')}
          testId="nothing-on-board"
        >
          {t(language, 'driver.problem.nothingOnBoard')}
        </Button>
      </Card>

      <Card>
        <Label>{t(language, 'driver.failed.title')}</Label>
        <textarea
          className={`mt-2 w-full rounded-xl border p-3 text-lg`}
          style={{ background: colors.surface2, borderColor: colors.border, color: colors.ink }}
          rows={3}
          value={note}
          onChange={(event) => setNote(event.target.value)}
        />
      </Card>
    </DriverLayout>
  )
}