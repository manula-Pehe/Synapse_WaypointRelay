import { useCallback, useEffect, useLayoutEffect, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AuthContext, type Credentials, type Language, type Session } from './auth'
import { isSession, readLanguage, readSession, saveLanguage, saveSession } from './session'
import { api, ApiError, configureApi } from '../lib/api'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(() => {
    const stored = readSession()
    if (stored?.token.startsWith('demo-')) return null
    return stored
  })
  const [language, setCurrentLanguage] = useState<Language>(
    () => session?.user.language ?? readLanguage(),
  )
  const logout = useCallback(() => {
    configureApi(null, () => {})
    saveSession(null)
    setSession(null)
    queryClient.clear()
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
    const next = { ...result, user: { ...result.user, language } }
    queryClient.clear()
    configureApi(next.token, logout)
    saveSession(next, remember)
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
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
