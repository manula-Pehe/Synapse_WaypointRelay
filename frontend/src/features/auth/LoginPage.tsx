import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { rolePaths, useAuth, type Language } from '../../app/auth'
import { useMessages } from '../../i18n/messages'
import { ApiError } from '../../lib/api'

// X1 / X1sm · Sign-in, matched to the supplied desktop and mobile references.
export function LoginPage() {
  const { user, login, language, setLanguage } = useAuth()
  const text = useMessages()
  const [identifier, setIdentifier] = useState('')
  const [secret, setSecret] = useState('')
  const [remember, setRemember] = useState(true)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<'invalid' | 'unavailable' | null>(null)
  if (user) return <Navigate to={rolePaths[user.role]} replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending) return
    setError(null)
    setPending(true)
    try {
      await login({ identifier: identifier.trim(), secret }, remember)
    } catch (error) {
      setError(error instanceof ApiError && error.status === 401 ? 'invalid' : 'unavailable')
    } finally {
      setPending(false)
    }
  }

  return (
    <main className="grid min-h-svh bg-canvas lg:grid-cols-[39%_61%]">
      <aside className="relative hidden flex-col bg-brand lg:flex px-6 py-7 text-on-brand lg:min-h-svh lg:px-[10%] lg:py-[10%]">
        <div className="flex items-center gap-3">
          <span className="flex size-12 items-center justify-center rounded-xl bg-surface text-brand">
            <Icon name="box" className="size-7" />
          </span>
          <div>
            <p className="text-xs font-bold tracking-[0.08em] text-brand-light">WAYPOINT</p>
            <p className="text-[22px] font-bold leading-7">Relay</p>
          </div>
        </div>
        <div className="hidden lg:block lg:mt-auto lg:pb-[15%] lg:pt-24">
          <h2 className="text-[clamp(32px,2.8vw,44px)] font-bold leading-[1.18] tracking-[-0.015em]">
            {text.promiseLine1}
            <br />
            {text.promiseLine2}
          </h2>
          <p className="mt-8 max-w-[490px] text-xl leading-[1.4] text-brand-light">{text.intro}</p>
          <ul className="mt-8 space-y-3 text-base font-medium">
            <li className="flex items-center gap-3">
              <Icon name="check" />
              {text.arrivalBenefit}
            </li>
            <li className="flex items-center gap-3">
              <Icon name="question" />
              {text.movedBenefit}
            </li>
            <li className="flex items-center gap-3">
              <Icon name="offline" />
              {text.offlineBenefit}
            </li>
          </ul>
        </div>
      </aside>
      <section
        className="flex min-h-svh items-stretch justify-center px-5 pb-[max(28px,env(safe-area-inset-bottom))] pt-[max(30px,env(safe-area-inset-top))] lg:items-center lg:px-12 lg:py-16"
        aria-labelledby="login-title"
      >
        <div className="flex w-full max-w-[480px] flex-col lg:block">
          <div className="grid grid-cols-[1fr_auto] items-center gap-x-3 gap-y-5 lg:flex lg:flex-wrap lg:justify-between lg:gap-4">
            <div className="flex items-center gap-2.5 lg:hidden">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand text-ink">
                <Icon name="box" className="size-5" />
              </span>
              <div>
                <p className="text-[11px] font-bold tracking-[0.06em] text-muted">WAYPOINT</p>
                <p className="text-base font-bold leading-5">Relay</p>
              </div>
            </div>
            <h1
              id="login-title"
              className="order-2 col-span-2 text-[28px] font-bold tracking-[-0.02em] lg:order-none lg:text-[30px]"
            >
              {text.signIn}
            </h1>
            <fieldset disabled={pending} className="flex rounded-xl bg-inset p-1">
              <legend className="sr-only">{text.language}</legend>
              {(
                [
                  ['en', 'English'],
                  ['si', 'සිංහල'],
                  ['ta', 'தமிழ்'],
                ] as [Language, string][]
              ).map(([code, label]) => (
                <button
                  key={code}
                  type="button"
                  lang={code}
                  aria-pressed={language === code}
                  onClick={() => setLanguage(code)}
                  className={`min-h-8 min-w-12 rounded-lg border px-3 text-sm lg:min-w-0 lg:min-h-9 lg:px-4 ${language === code ? 'border-line bg-surface font-semibold text-ink shadow-xs' : 'border-transparent text-muted'}`}
                >
                  {code === 'en' ? (
                    <>
                      <span className="lg:hidden">EN</span>
                      <span className="hidden lg:inline">{label}</span>
                    </>
                  ) : (
                    label
                  )}
                </button>
              ))}
            </fieldset>
          </div>
          <p className="mt-5 text-[15px] leading-6 tracking-[-0.015em] text-muted lg:text-base lg:tracking-normal">
            <span className="lg:hidden">{text.mobileSubtitle}</span>
            <span className="hidden lg:inline">{text.subtitle}</span>
          </p>
          <form
            onSubmit={submit}
            className="mt-5 flex flex-1 flex-col lg:mt-6 lg:block"
            aria-busy={pending}
          >
            <label htmlFor="identifier" className="mb-2 block text-sm font-semibold">
              <span className="lg:hidden">{text.mobileEmail}</span>
              <span className="hidden lg:inline">{text.identifier}</span>
            </label>
            <div className="relative">
              <span className="pointer-events-none absolute inset-y-0 left-4 flex items-center text-muted">
                <Icon name="user" />
              </span>
              <input
                id="identifier"
                name="identifier"
                autoComplete="username"
                autoCapitalize="none"
                spellCheck={false}
                required
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                disabled={pending}
                className="h-12 w-full rounded-[11px] border lg:h-[52px] border-line bg-surface pl-12 pr-4 text-base focus:border-brand focus:outline-2 focus:outline-offset-0 focus:outline-brand"
              />
            </div>
            <label htmlFor="secret" className="mb-2 mt-6 block text-sm font-semibold">
              {text.secret}
            </label>
            <div className="relative">
              <span className="pointer-events-none absolute inset-y-0 left-4 flex items-center text-muted">
                <Icon name="lock" />
              </span>
              <input
                id="secret"
                name="secret"
                type="password"
                autoComplete="current-password"
                required
                value={secret}
                onChange={(e) => setSecret(e.target.value)}
                disabled={pending}
                className="h-12 w-full rounded-[11px] border lg:h-[52px] border-line bg-surface pl-12 pr-4 text-base focus:border-brand focus:outline-2 focus:outline-offset-0 focus:outline-brand"
              />
            </div>
            <p className="mt-2 hidden text-sm leading-5 text-muted lg:block">{text.forgot}</p>
            <label className="mb-8 mt-4 flex min-h-9 lg:my-4 cursor-pointer items-center gap-3 text-base">
              <input
                type="checkbox"
                checked={remember}
                onChange={(e) => setRemember(e.target.checked)}
                disabled={pending}
                className="size-[18px] shrink-0 accent-brand"
              />
              <span className="lg:hidden">{text.mobileRemember}</span>
              <span className="hidden lg:inline">{text.remember}</span>
            </label>
            {error && (
              <p role="alert" className="mb-4 rounded-lg bg-danger-soft p-3 text-sm text-danger">
                {text[error]}
              </p>
            )}
            <button
              disabled={pending}
              className="mt-auto min-h-[52px] w-full shrink-0 rounded-[11px] bg-brand py-3 lg:mt-0 px-4 font-semibold text-on-brand hover:brightness-95 disabled:opacity-60"
            >
              {pending ? text.signingIn : text.signIn}
            </button>
          </form>
          <p className="mt-5 text-center text-sm leading-5 text-muted lg:hidden">
            {text.mobileForgot}
          </p>
          <div className="mt-5 hidden items-start gap-3 rounded-xl bg-inset p-4 text-sm leading-5 text-muted lg:flex">
            <Icon name="info" className="mt-px size-[18px] shrink-0" />
            <p>{text.deviceNote}</p>
          </div>
        </div>
      </section>
    </main>
  )
}

function Icon({
  name,
  className = 'size-5 shrink-0',
}: {
  name: 'box' | 'user' | 'lock' | 'check' | 'question' | 'offline' | 'info'
  className?: string
}) {
  const paths = {
    box: (
      <>
        <path d="m12 3 9 4.5v9L12 21l-9-4.5v-9L12 3Z" />
        <path d="m3 7.5 9 4.5 9-4.5M12 12v9M7.5 5.25l9 4.5" />
      </>
    ),
    user: (
      <>
        <circle cx="12" cy="7" r="4" />
        <path d="M4 21v-2a8 8 0 0 1 16 0v2" />
      </>
    ),
    lock: (
      <>
        <rect x="4" y="10" width="16" height="12" rx="2" />
        <path d="M8 10V6a4 4 0 0 1 8 0v4" />
      </>
    ),
    check: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="m8 12 3 3 5-6" />
      </>
    ),
    question: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M9.5 8a2.5 2.5 0 1 1 4 2c-1.5 1-1.5 1-1.5 3M12 16h.01" />
      </>
    ),
    info: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 11v6M12 7h.01" />
      </>
    ),
    offline: (
      <>
        <path d="m3 3 18 18M6 8a6 6 0 0 1 11 2 4 4 0 0 1 3 6M6 9a5 5 0 0 0 0 10h12" />
      </>
    ),
  }
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name]}
    </svg>
  )
}
