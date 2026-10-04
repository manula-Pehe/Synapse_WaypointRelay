import { useCallback, useEffect, useLayoutEffect, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AuthContext, type Credentials, type Language, type Session } from './auth'
import { isSession, readLanguage, readSession, saveLanguage, saveSession } from './session'
import { api, ApiError, configureApi } from '../lib/api'
import { checkPinOffline, forgetPin, rememberPin } from '../lib/offline'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(() => {
    const stored = readSession()
    if (stored?.token.startsWith('demo-')) return null
    return stored
  })
  const [language, setCurrentLanguage] = useState<Language>(
    () => session && (session.user.role === 'DISPATCHER' || session.user.role === 'LOADER') ? 'en' : session?.user.language ?? readLanguage(),
  )
  const logout = useCallback(() => {
    configureApi(null, () => {})
    saveSession(null)
    setSession(null)
    queryClient.clear()
    // Signing out ends this driver's trusted offline access, so the stored PIN goes with it.
    forgetPin()
  }, [queryClient])

  useLayoutEffect(() => {
    configureApi(session?.token ?? null, logout)
    return () => configureApi(null, () => {})
  }, [session, logout])
  useEffect(() => {
    document.documentElement.lang = language
  }, [language])

  function setLanguage(next: Language) {
    setCurrentLanguage(next)
    saveLanguage(next)
    if (session) {
      const updated = { ...session, user: { ...session.user, language: next } }
      saveSession(updated)
      setSession(updated)
    }
  }
  async function login(credentials: Credentials, remember = true) {
    const result = await api<Session>('auth/login', {
          method: 'POST',
          authenticated: false,
          body: JSON.stringify(credentials),
        })
    if (!isSession(result))
      throw new ApiError(502, 'INVALID_SESSION', 'The server returned an invalid sign-in response.')
    const nextLanguage = result.user.role === 'DISPATCHER' || result.user.role === 'LOADER' ? 'en' : language
    const next = { ...result, user: { ...result.user, language: nextLanguage } }
    setCurrentLanguage(nextLanguage)
    queryClient.clear()
    configureApi(next.token, logout)
    saveSession(next, remember)
    setSession(next)
    // X1m-off - keep a salted hash of the PIN so the driver can sign in at a depot with no signal.
    // Only ever after the server has accepted these credentials.
    if (next.user.role === 'DRIVER') {
      await rememberPin(
        {
          staffId: credentials.identifier,
          userId: next.user.id,
          name: next.user.name,
          vehicleId: next.user.vehicleId,
        },
        credentials.secret,
      )
    }
  }

  /**
   * X1m-off - sign in with no network, against the hash stored at the last online sign-in.
   *
   * This mints a session the server has never seen, marked with an `offline-` token so every caller
   * can tell the difference. It carries no authority: no endpoint accepts it, and writes go to the
   * outbox until a real token is back. That is the whole design - offline work is queued and
   * reconciled later, never assumed.
   */
  async function loginOffline(credentials: Credentials): Promise<void> {
    const check = await checkPinOffline(credentials.identifier.trim(), credentials.secret)
    if (!check.ok || !check.credential) {
      throw new ApiError(401, 'INVALID_CREDENTIALS', 'That PIN does not match the last online sign-in.')
    }
    const { userId, name, vehicleId } = check.credential
    const next: Session = {
      token: `offline-${userId}`,
      user: {
        id: userId,
        name,
        role: 'DRIVER',
        outletId: null,
        depot: null,
        vehicleId,
        language,
      },
    }
    queryClient.clear()
    configureApi(null, () => {})
    saveSession(next)
    setSession(next)
  }

  return (
    <AuthContext.Provider
      value={{
        user: session?.user ?? null,
        token: session?.token ?? null,
        language,
        setLanguage,
        login,
        loginOffline,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
