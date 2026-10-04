import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, Navigate } from 'react-router-dom'
import { rolePaths, useAuth } from '../../app/auth'
import { api, ApiError } from '../../lib/api'

export function LoaderSignIn() {
  const { user, login } = useAuth()
  const [pin, setPin] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const settings = useQuery({
    queryKey: ['loader-public-settings'],
    queryFn: () => api<{ now: string }>('settings', { authenticated: false }),
    refetchInterval: 30_000,
  })
  const clock = settings.data
    ? new Date(settings.data.now).toLocaleTimeString('en-LK', {
        timeZone: 'Asia/Colombo',
        hour: 'numeric',
        minute: '2-digit',
      })
    : '-'
  if (user) return <Navigate to={rolePaths[user.role]} replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pin.length !== 4 || pending) return
    setPending(true)
    setError('')
    try {
      await login({ identifier: 'PELIYAGODA', secret: pin }, false)
    } catch (cause) {
      setPin('')
      setError(
        cause instanceof ApiError && cause.status === 401
          ? 'That PIN did not match. Try again.'
          : 'Sign-in is unavailable. Try again.',
      )
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="flex min-h-svh flex-col bg-canvas text-ink">
      <header className="border-b border-line bg-surface px-5 py-3">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3">
          <div>
            <span className="mr-2 inline-flex h-9 w-9 items-center justify-center rounded-lg bg-brand text-xl text-on-brand">
              ⬡
            </span>
            <span className="font-bold">Waypoint Relay</span>
            <span className="ml-4 text-sm text-muted">Dock · Peliyagoda</span>
          </div>
          <div className="flex items-center gap-3">
            <strong>{clock}</strong>
            <span
              className={`rounded-full px-3 py-1 text-xs font-semibold ${settings.data ? 'bg-status-delivered-soft text-status-delivered' : settings.error ? 'bg-status-offline-soft text-status-offline' : 'bg-surface-2 text-muted'}`}
            >
              {settings.data ? '✓ Online' : settings.error ? '○ Offline' : '○ Checking'}
            </span>
          </div>
        </div>
      </header>
      <main className="flex flex-1 items-center justify-center px-5 py-10">
        <form onSubmit={submit} className="w-full max-w-xs text-center" aria-busy={pending}>
          <h1 className="text-2xl font-bold">Enter your 4-digit PIN</h1>
          <p className="mt-2 text-sm text-muted">
            This tablet is shared. Your PIN keeps your work under your name.
          </p>
          <label htmlFor="loader-pin" className="sr-only">
            Four-digit PIN
          </label>
          <input
            id="loader-pin"
            type="password"
            inputMode="numeric"
            autoComplete="off"
            pattern="[0-9]{4}"
            maxLength={4}
            value={pin}
            onChange={(event) => {
              setError('')
              setPin(event.target.value.replace(/\D/g, '').slice(0, 4))
            }}
            className="sr-only"
            aria-describedby={error ? 'loader-pin-error' : undefined}
          />
          <div
            className="my-6 flex justify-center gap-3"
            aria-label={`${pin.length} of 4 digits entered`}
          >
            {[0, 1, 2, 3].map((index) => (
              <span
                key={index}
                className={`h-4 w-4 rounded-full border-2 border-brand ${index < pin.length ? 'bg-brand' : 'bg-surface'}`}
              />
            ))}
          </div>
          <div className="grid grid-cols-3 gap-3" aria-label="PIN keypad">
            {['1', '2', '3', '4', '5', '6', '7', '8', '9', '⌫', '0', '✓'].map((key) => (
              <button
                key={key}
                type={key === '✓' ? 'submit' : 'button'}
                disabled={pending || (key === '✓' && pin.length !== 4)}
                className={`min-h-14 rounded-xl border text-xl font-semibold disabled:opacity-50 ${key === '✓' ? 'border-brand bg-brand text-on-brand' : 'border-line bg-surface active:bg-brand-soft'}`}
                aria-label={key === '⌫' ? 'Delete last digit' : key === '✓' ? 'Sign in' : key}
                onClick={() => {
                  if (key !== '✓') {
                    setError('')
                    setPin((current) =>
                      key === '⌫' ? current.slice(0, -1) : (current + key).slice(0, 4),
                    )
                  }
                }}
              >
                {key}
              </button>
            ))}
          </div>
          {error && (
            <p
              id="loader-pin-error"
              role="alert"
              className="mt-5 rounded-lg bg-danger-soft p-3 text-danger"
            >
              {error}
            </p>
          )}
          {pending && <p className="mt-4 text-sm text-muted">Signing in…</p>}
          <Link
            to="/login"
            className="mt-7 inline-flex min-h-14 items-center text-sm font-semibold text-brand underline"
          >
            Sign in as another role
          </Link>
        </form>
      </main>
    </div>
  )
}
