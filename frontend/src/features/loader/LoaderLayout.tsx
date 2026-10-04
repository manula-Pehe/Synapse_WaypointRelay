import { useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../app/auth'
import { api } from '../../lib/api'

interface Settings {
  now: string
  runDate: string
  timezone: string
}

export function LoaderLayout({ children, context }: { children: ReactNode; context?: string }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const settings = useQuery({
    queryKey: ['loader-settings'],
    queryFn: () => api<Settings>('settings'),
    refetchInterval: 30_000,
  })
  const clock = settings.data
    ? new Date(settings.data.now).toLocaleTimeString('en-LK', {
        timeZone: 'Asia/Colombo',
        hour: 'numeric',
        minute: '2-digit',
      })
    : '-'
  const switchLoader = () => {
    logout()
    navigate('/loader/sign-in', { replace: true })
  }
  return (
    <div className="min-h-svh bg-canvas text-ink">
      <header className="border-b border-line bg-surface px-4 py-2">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-3">
          <div className="flex min-w-0 flex-1 items-center gap-3">
            <span
              aria-hidden="true"
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand text-xl text-on-brand"
            >
              ⬡
            </span>
            <strong className="whitespace-nowrap">Waypoint Relay</strong>
            <span className="truncate text-sm text-muted">
              {context ?? `Dock · ${user?.depot ?? 'Peliyagoda'}`}
            </span>
          </div>
          <strong className="whitespace-nowrap text-sm">{clock}</strong>
          <span
            className={`rounded-full px-3 py-1 text-xs font-semibold ${settings.error ? 'bg-status-offline-soft text-status-offline' : settings.data ? 'bg-status-delivered-soft text-status-delivered' : 'bg-surface-2 text-muted'}`}
          >
            {settings.error ? '○ Offline' : settings.data ? '✓ Online' : '○ Checking'}
          </span>
          <button
            className="min-h-14 rounded-xl border border-line px-3 text-sm font-semibold"
            onClick={switchLoader}
          >
            {user?.name ?? 'Loader'} · Switch
          </button>
          <div className="relative">
            <button
              className="min-h-14 min-w-14 rounded-xl border border-line text-xl"
              aria-label="Dock menu"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((open) => !open)}
            >
              ☰
            </button>
            {menuOpen && (
              <div className="absolute right-0 z-20 mt-2 w-48 rounded-xl border border-line bg-surface p-2 shadow-lg">
                <button
                  className="min-h-14 w-full rounded-lg px-3 text-left font-semibold"
                  onClick={switchLoader}
                >
                  Switch loader
                </button>
              </div>
            )}
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl p-4 sm:p-6">{children}</main>
    </div>
  )
}
