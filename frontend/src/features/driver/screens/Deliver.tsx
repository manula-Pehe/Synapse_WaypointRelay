import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { enqueue } from '../../../lib/offline'
import { driverApi } from '../api'
import DriverLayout from '../DriverLayout'
import { Button, Card, Label, TOUCH, Value } from '../components'
import { t, type Language } from '../i18n'
import { outletLabel } from '../outlet'
import { dataUrlToFile } from '../dataUrl'
import { SignaturePad } from '../signature'
import { useDriverTheme } from '../theme'
import type { DriverStop } from '../types'

export interface DeliverProps {
  stop: DriverStop
  language: Language
  onToggleTheme: () => void
  onRecorded: () => void
}

/**
 * R4p, R4s and R4 - recording a delivery with its proof.
 *
 * Nothing here calls a write endpoint. The whole thing is one enqueue, so the driver sees the result
 * the moment they tap, and the phone keeps it until there is a signal. The photo uses
 * `<input capture>` so it opens the camera rather than the gallery.
 */
export default function Deliver({ stop, language, onToggleTheme, onRecorded }: DeliverProps) {
  const { colors } = useDriverTheme()
  const [receivedBy, setReceivedBy] = useState('')
  const [photo, setPhoto] = useState<File | null>(null)
  const [signature, setSignature] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const { undoWindow, startUndo, cancelUndo } = useUndoWindow()
  const navigate = useNavigate()

  async function record() {
    setSaving(true)
    setError(null)
    try {
      // Proof goes up first, because the delivery names the file ids. Both uploads are
      // idempotent on clientId, so a retry after a dropped connection does not double them up.
      const photoFileId = photo ? (await driverApi.uploadProof(photo, 'photo', crypto.randomUUID())).id : null
      const signatureFileId = signature
        ? (await driverApi.uploadProof(dataUrlToFile(signature), 'signature', crypto.randomUUID())).id
        : null

      await enqueue('DELIVERY_RECORDED', {
        stopId: stop.id,
        orderId: stop.orderId ?? stop.orderRef,
        outcome: 'DELIVERED',
        units: stop.cases,
        ...(receivedBy ? { receivedBy } : {}),
        ...(photoFileId ? { photoFileId } : {}),
        ...(signatureFileId ? { signatureFileId } : {}),
        completedAt: new Date().toISOString(),
      })

      onRecorded()
      setSaved(true)
      startUndo()
    } catch {
      // Nothing has been recorded yet, so the driver can simply try again.
      setError(t(language, 'driver.delivery.retry'))
    } finally {
      setSaving(false)
    }
  }

  async function undo() {
    await enqueue('DELIVERY_UNDONE', { deliveryId: stop.id })
    cancelUndo()
    setSaved(false)
  }

  if (saved) {
    return (
      <DriverLayout
        title={t(language, 'driver.delivery.deliver')}
        language={language}
        onToggleTheme={onToggleTheme}
        action={
          <Button full onClick={() => navigate('/driver/stops')} testId="back-to-stops">
            {t(language, 'driver.stops.title')}
          </Button>
        }
      >
        <Card>
          <Label>{outletLabel(stop.outlet)}</Label>
          <Value size="lg">
            {stop.cases} {t(language, 'driver.stops.cases')}
          </Value>
        </Card>

        <Card>
          <p style={{ color: colors.ok }} data-testid="saved-locally">
            {t(language, 'driver.delivery.saved')}
          </p>
        </Card>

        {undoWindow > 0 && (
          <Button variant="secondary" full onClick={undo} testId="undo">
            {t(language, 'driver.delivery.undo')} · {undoWindow}
          </Button>
        )}
      </DriverLayout>
    )
  }

  return (
    <DriverLayout
      title={`${stop.sequence}. ${outletLabel(stop.outlet)}`}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => navigate(`/driver/stop/${stop.id}`)}
      action={
        <Button
          full
          onClick={() => void record()}
          disabled={receivedBy.trim().length === 0 || saving}
          testId="record-delivery"
        >
          {t(language, 'driver.delivery.deliver')}
        </Button>
      }
    >
      {error && (
        <p style={{ color: colors.danger }} data-testid="deliver-error">
          {error}
        </p>
      )}
      <Card>
        <Label>{stop.orderRef}</Label>
        <Value size="lg">
          {stop.cases} {t(language, 'driver.stops.cases')}
        </Value>
      </Card>

      <label className="block space-y-2">
        <span className="text-sm" style={{ color: colors.ink2 }}>
          {t(language, 'driver.delivery.receivedBy')}
        </span>
        <input
          className={`w-full ${TOUCH} rounded-xl border px-4 text-xl`}
          style={{ background: colors.surface2, borderColor: colors.border, color: colors.ink }}
          value={receivedBy}
          onChange={(event) => setReceivedBy(event.target.value)}
          data-testid="received-by"
        />
      </label>

      <Card>
        <Label>{t(language, 'driver.delivery.photo')}</Label>
        <input
          type="file"
          accept="image/*"
          capture="environment"
          className="mt-2 w-full text-sm"
          onChange={(event) => setPhoto(event.target.files?.[0] ?? null)}
          data-testid="photo"
        />
      </Card>

      <Card>
        <Label>{t(language, 'driver.delivery.signature')}</Label>
        <SignaturePad onSigned={setSignature} />
      </Card>

      <Button
        variant="ghost"
        full
        onClick={() => navigate(`/driver/stop/${stop.id}/failed`)}
        testId="to-failed"
      >
        {t(language, 'driver.failed.short')}
      </Button>
    </DriverLayout>
  )
}

/** R4b - the 10-second undo. A timer, not a confirm dialog, so the common case stays fast. */
function useUndoWindow(seconds = 10) {
  const [left, setLeft] = useState(0)
  const cancel = useRef<number | undefined>(undefined)

  useEffect(() => () => window.clearInterval(cancel.current), [])

  return {
    undoWindow: left,
    startUndo() {
      setLeft(seconds)
      window.clearInterval(cancel.current)
      cancel.current = window.setInterval(() => {
        setLeft((current) => {
          if (current <= 1) {
            window.clearInterval(cancel.current)
            return 0
          }
          return current - 1
        })
      }, 1000)
    },
    cancelUndo() {
      window.clearInterval(cancel.current)
      setLeft(0)
    },
  }
}
