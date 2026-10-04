import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../../app/auth'
import { ApiError } from '../../../lib/api'
import DriverLayout from '../DriverLayout'
import { Button, TOUCH } from '../components'
import { t, type Language } from '../i18n'
import { useDriverTheme } from '../theme'

export interface SignInProps {
  language: Language
  onToggleTheme: () => void
  /** True when the phone has no signal, which switches the screen to X1m-off: PIN only. */
  offline: boolean
}

/**
 * X1m and X1m-off.
 *
 * The PIN is the driver's staff id and secret, the same pair every other role uses, so signing in
 * here goes through the shared `AuthProvider` and the session that comes back is the one the rest of
 * the app already trusts. The offline case is the one that matters: at a depot with no signal the
 * driver still has to start a run, so a remembered staff id is offered and the actions queue until
 * there is a network.
 */
export default function SignIn({ language, onToggleTheme, offline }: SignInProps) {
  const { user, login, loginOffline } = useAuth()
  const [staffId, setStaffId] = useState(offline ? (user?.name ?? '') : '')
  const [pin, setPin] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const { colors } = useDriverTheme()
  const navigate = useNavigate()

  const field = `w-full ${TOUCH} rounded-xl border px-4 text-xl`

  async function signIn() {
    if (pin.length < 4) {
      setError(t(language, 'driver.signin.pin'))
      return
    }
    setBusy(true)
    setError(null)
    const credentials = { identifier: staffId.trim(), secret: pin }
    try {
      // With no signal the PIN is checked against the hash stored at the last online sign-in. The
      // session that comes back carries no authority — actions queue in the outbox until the
      // server has seen them.
      if (offline) await loginOffline(credentials)
      else await login(credentials)
      navigate('/driver/today', { replace: true })
    } catch (cause) {
      // 401 is a wrong PIN; anything else is the depot's network, which the offline banner covers.
      setError(cause instanceof ApiError && cause.status === 401 ? 'invalid' : 'unavailable')
      setBusy(false)
    }
  }

  return (
    <DriverLayout
      title={t(language, 'driver.app.title')}
      language={language}
      onToggleTheme={onToggleTheme}
    >
      {offline && (
        <div
          className="rounded-xl border p-3 text-sm"
          style={{ color: colors.warn, borderColor: colors.warn }}
          data-testid="offline-notice"
        >
          {t(language, 'driver.signin.offline')}
        </div>
      )}

      <label className="block space-y-2">
        <span className="text-sm" style={{ color: colors.ink2 }}>
          {t(language, 'driver.signin.staffId')}
        </span>
        <input
          className={field}
          style={{ background: colors.surface, borderColor: colors.border, color: colors.ink }}
          value={staffId}
          autoCapitalize="characters"
          onChange={(event) => setStaffId(event.target.value)}
        />
      </label>

      <label className="block space-y-2">
        <span className="text-sm" style={{ color: colors.ink2 }}>
          {t(language, 'driver.signin.pin')}
        </span>
        <input
          className={field}
          style={{ background: colors.surface, borderColor: colors.border, color: colors.ink }}
          type="password"
          inputMode="numeric"
          value={pin}
          onChange={(event) => setPin(event.target.value)}
          onKeyDown={(event) => event.key === 'Enter' && signIn()}
        />
      </label>

      {error && (
        <p className="text-sm" style={{ color: colors.danger }} role="alert">
          {error === 'invalid' ? 'That staff ID and PIN do not match.' : 'Could not reach the depot. Try again.'}
        </p>
      )}

      <Button full onClick={signIn} testId="sign-in" disabled={busy}>
        {busy ? '…' : t(language, 'driver.signin.continue')}
      </Button>
    </DriverLayout>
  )
}