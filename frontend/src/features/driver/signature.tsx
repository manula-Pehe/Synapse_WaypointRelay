import { useRef, useState } from 'react'
import { useDriverTheme } from './theme'

/**
 * R4s - a simple signature pad.
 *
 * A pointer-events canvas rather than a signature library - the brief allows adding one, but a pad
 * this simple is a few lines and keeps the offline bundle small on a cheap phone.
 *
 * It hands back a data URL; `dataUrlToFile` turns that into bytes for the upload.
 */
export function SignaturePad({ onSigned }: { onSigned: (dataUrl: string | null) => void }) {
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
