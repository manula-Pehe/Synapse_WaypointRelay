import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { enqueue } from '../../../lib/offline'
import DriverLayout from '../DriverLayout'
import { Button, Card, Label, TOUCH, Value } from '../components'
import { t, type Language } from '../i18n'
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
  const [photo, setPhoto] = useState<string | null>(null)
  const [signature, setSignature] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const { undoWindow, startUndo, cancelUndo } = useUndoWindow()
  const navigate = useNavigate()

  async function record() {
    await enqueue('DELIVERY_RECORDED', {
      stopId: stop.id,
      orderId: stop.orderRef,
      outcome: 'DELIVERED',
      units: stop.cases,
      ...(receivedBy ? { receivedBy } : {}),
      ...(photo ? { photoFileId: photo } : {}),
      ...(signature ? { signatureFileId: signature } : {}),
      completedAt: new Date().toISOString(),
    })

    onRecorded()
    setSaved(true)
    startUndo()
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
          <Label>{stop.outlet.id}</Label>
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
      title={`${stop.sequence}. ${stop.outlet.id}`}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => navigate(`/driver/stop/${stop.id}`)}
      action={
        <Button full onClick={record} disabled={receivedBy.trim().length === 0} testId="record-delivery">
          {t(language, 'driver.delivery.deliver')}
        </Button>
      }
    >
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
          onChange={(event) => setPhoto(event.target.files?.[0]?.name ?? null)}
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

/**
 * R4s — a simple signature pad.
 * A pointer-events canvas rather than a signature library - the brief allows adding one, but a pad
 * this simple is a few lines and keeps the offline bundle small on a cheap phone.
 */
function SignaturePad({ onSigned }: { onSigned: (dataUrl: string | null) => void }) {
  const canvas = useRef<HTMLCanvasElement>(null)
  const drawing = useRef(false)
  const [hasInk, setHasInk] = useState(false)
  const { colors } = useDriverTheme()

  function start(event: React.PointerEvent<HTMLCanvasElement>) {
    drawing.current = true
    const context = canvas.current?.getContext('2d')
    if (context) {
      // Canvas ink is black unless it is set, which is invisible on the dark pad.
      context.strokeStyle = colors.ink
      context.lineWidth = 2
      context.beginPath()
      context.moveTo(event.nativeEvent.offsetX, event.nativeEvent.offsetY)
    }
  }

  function move(event: React.PointerEvent<HTMLCanvasElement>) {
    if (!drawing.current) return
    const context = canvas.current?.getContext('2d')
    if (!context) return
    context.lineTo(event.nativeEvent.offsetX, event.nativeEvent.offsetY)
    context.stroke()
    setHasInk(true)
  }

  function end() {
    if (!drawing.current) return
    drawing.current = false
    const element = canvas.current
    if (element && hasInk) onSigned(element.toDataURL('image/png'))
  }

  return (
    <canvas
      ref={canvas}
      width={320}
      height={140}
      data-testid="signature"
      onPointerDown={start}
      onPointerMove={move}
      onPointerUp={end}
      onPointerLeave={end}
      className="mt-2 w-full touch-none rounded-xl border"
      style={{ background: colors.surface2, borderColor: colors.border }}
    />
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