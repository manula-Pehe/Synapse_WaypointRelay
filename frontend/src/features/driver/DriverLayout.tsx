import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import SyncPill from './SyncPill'
import { useDriverTheme } from './theme'
import { t, type Language } from './i18n'
import { TOUCH } from './components'

export interface DriverLayoutProps {
  title: string
  language: Language
  onToggleTheme: () => void
  /** The one main action, pinned to the bottom so it is reachable with a thumb. */
  action?: ReactNode
  onBack?: () => void
  children: ReactNode
}

/**
 * The shell every driver screen sits in: title, sync badge, sun/moon switch and menu (F2).
 * Dark by default, 64px targets, phone width.
 */
export default function DriverLayout({
  title,
  language,
  onToggleTheme,
  action,
  onBack,
  children,
}: DriverLayoutProps) {
  const { colors, theme } = useDriverTheme()
  const navigate = useNavigate()

  return (
    <div className="flex min-h-dvh flex-col" style={{ background: colors.bg, color: colors.ink }}>
      <header
        className="sticky top-0 z-10 border-b px-4"
        style={{ background: colors.bg, borderColor: colors.border }}
      >
        <div className="flex items-center gap-2 py-3">
          {onBack && (
            <button
              type="button"
              aria-label="Back"
              onClick={onBack}
              className={`flex ${TOUCH} w-16 shrink-0 items-center justify-center text-2xl`}
              style={{ color: colors.ink2 }}
            >
              ‹
            </button>
          )}
          <h1 className="min-w-0 flex-1 truncate text-lg font-semibold">{title}</h1>
          <SyncPill language={language} />
          <button
            type="button"
            aria-label={t(language, 'driver.menu.theme')}
            onClick={onToggleTheme}
            className={`flex ${TOUCH} w-16 shrink-0 items-center justify-center text-xl`}
            style={{ color: colors.ink2 }}
          >
            {theme === 'dark' ? '☀' : '☾'}
          </button>
          <button
            type="button"
            aria-label={t(language, 'driver.menu.title')}
            onClick={() => navigate('/driver/menu')}
            className={`flex ${TOUCH} w-16 shrink-0 items-center justify-center text-xl`}
            style={{ color: colors.ink2 }}
          >
            ☰
          </button>
        </div>
      </header>

      <main className="flex-1 space-y-3 px-4 py-4">{children}</main>

      {action && (
        <div
          className="sticky bottom-0 border-t p-4"
          style={{ background: colors.bg, borderColor: colors.border }}
        >
          {action}
        </div>
      )}
    </div>
  )
}