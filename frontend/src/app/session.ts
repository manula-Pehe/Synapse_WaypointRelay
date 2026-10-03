import { rolePaths, type Language, type Session } from './auth'

const sessionKey = 'waypoint.session'
const languageKey = 'waypoint.language'
export function isLanguage(value: unknown): value is Language {
  return value === 'en' || value === 'si' || value === 'ta'
}
export function isSession(value: unknown): value is Session {
  if (!value || typeof value !== 'object') return false
  const { token, user } = value as Partial<Session>
  return (
    typeof token === 'string' &&
    token.length > 0 &&
    !!user &&
    typeof user.id === 'string' &&
    typeof user.name === 'string' &&
    Object.hasOwn(rolePaths, user.role) &&
    isLanguage(user.language)
  )
}
export function readSession(): Session | null {
  try {
    const value: unknown = JSON.parse(
      sessionStorage.getItem(sessionKey) ?? localStorage.getItem(sessionKey) ?? 'null',
    )
    return isSession(value) ? value : null
  } catch {
    return null
  }
}
export function saveSession(session: Session | null, remember?: boolean) {
  try {
    const persistent = remember ?? localStorage.getItem(sessionKey) !== null
    localStorage.removeItem(sessionKey)
    sessionStorage.removeItem(sessionKey)
    if (session)
      (persistent ? localStorage : sessionStorage).setItem(sessionKey, JSON.stringify(session))
  } catch {
    /* Keep the session in memory when browser storage is unavailable. */
  }
}
export function readLanguage(): Language {
  try {
    const language = localStorage.getItem(languageKey)
    return isLanguage(language) ? language : 'en'
  } catch {
    return 'en'
  }
}
export function saveLanguage(language: Language) {
  try {
    localStorage.setItem(languageKey, language)
  } catch {
    /* Keep in memory. */
  }
}
