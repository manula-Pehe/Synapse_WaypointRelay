import type { CSSProperties, ReactNode } from 'react'
import { useDriverTheme } from './theme'

/** 64px is the driver touch target from the design system - a tap has to work with gloves on. */
export const TOUCH = 'min-h-16'

export interface ButtonProps {
  children: ReactNode
  onClick?: () => void
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  disabled?: boolean
  full?: boolean
  testId?: string
}

/** One main action per screen, so the primary variant is reserved for it. */
export function Button({
  children,
  onClick,
  variant = 'primary',
  disabled,
  full,
  testId,
}: ButtonProps) {
  const { colors } = useDriverTheme()
  const base = `inline-flex items-center justify-center gap-2 rounded-xl px-5 text-xl/8 font-medium transition-opacity ${TOUCH} ${
    full ? 'w-full' : ''
  } ${disabled ? 'opacity-40' : 'active:opacity-70'}`

  const tone = {
    primary: { background: colors.brand, color: colors.onAccent },
    secondary: { background: colors.surface2, color: colors.ink },
    danger: { background: colors.danger, color: colors.onAccent },
    ghost: { background: 'transparent', color: colors.ink2 },
  }[variant]

  return (
    <button
      type="button"
      data-testid={testId}
      onClick={onClick}
      disabled={disabled}
      style={tone}
      className={base}
    >
      {children}
    </button>
  )
}

export function Card({
  children,
  className = '',
  style,
}: {
  children: ReactNode
  className?: string
  style?: CSSProperties
}) {
  const { colors } = useDriverTheme()
  return (
    <div
      className={`rounded-2xl border p-4 ${className}`}
      style={{ background: colors.surface, borderColor: colors.border, ...style }}
    >
      {children}
    </div>
  )
}

export function Label({ children }: { children: ReactNode }) {
  const { colors } = useDriverTheme()
  return (
    <div className="text-sm tracking-wide uppercase" style={{ color: colors.ink2 }}>
      {children}
    </div>
  )
}

export function Value({ children, size = 'base' }: { children: ReactNode; size?: 'base' | 'lg' }) {
  const { colors } = useDriverTheme()
  return (
    <div className={size === 'lg' ? 'text-3xl font-semibold' : 'text-xl'} style={{ color: colors.ink }}>
      {children}
    </div>
  )
}

export function Screen({ children }: { children: ReactNode }) {
  const { colors } = useDriverTheme()
  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-[390px] flex-col" style={{ color: colors.ink }}>
      {children}
    </div>
  )
}

/** A pill that reads at a glance in a cab - this is why the count is in the badge, not the title. */
export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'brand' | 'warn' | 'ok' | 'danger' }) {
  const { colors } = useDriverTheme()
  const toneColor = {
    neutral: colors.ink2,
    brand: colors.brand,
    warn: colors.warn,
    ok: colors.ok,
    danger: colors.danger,
  }[tone]

  return (
    <span
      className="rounded-full border px-3 py-1 text-sm font-medium whitespace-nowrap"
      style={{ color: toneColor, borderColor: toneColor }}
    >
      {children}
    </span>
  )
}