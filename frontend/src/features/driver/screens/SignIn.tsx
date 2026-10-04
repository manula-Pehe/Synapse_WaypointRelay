import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
 * The offline case is the one that matters: at a depot with no signal the driver still has to start
 * a run, so the PIN they used last time is checked on the phone and the actions queue until there
 * is a network. The check is a stand-in for the real hash comparison - it only decides which
 * screen to show.
 */
export default function SignIn({ language, onToggleTheme, offline }: SignInProps) {
  const [staffId, setStaffId] = useState(offline ? 'DRV-014' : '')
  const [pin, setPin] = useState('')
  const [error, setError] = useState<string | null>(null)
  const { colors } = useDriverTheme()
  const navigate = useNavigate()

  const field = `w-full ${TOUCH} rounded-xl border px-4 text-xl`

  function signIn() {
    if (pin.length < 4) {
      setError(t(language, 'driver.signin.pin'))
      return
    }
    navigate('/driver/today')
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
          {error}
        </p>
      )}

      <Button full onClick={signIn} testId="sign-in">
        {t(language, 'driver.signin.continue')}
      </Button>
    </DriverLayout>
  )
}