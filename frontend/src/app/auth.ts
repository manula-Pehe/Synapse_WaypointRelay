import { createContext, useContext } from 'react'

export const rolePaths = {
  STORE_MANAGER: '/store',
  DISPATCHER: '/dispatch',
  LOADER: '/loader',
  DRIVER: '/driver',
} as const
export type Role = keyof typeof rolePaths
export type Language = 'en' | 'si' | 'ta'
export interface User {
  id: string
  name: string
  role: Role
  outletId: string | null
  depot: string | null
  vehicleId: string | null
  language: Language
}
export interface Session {
  token: string
  user: User
}
export interface Credentials {
  identifier: string
  secret: string
}
export interface AuthValue {
  user: User | null
  token: string | null
  language: Language
  setLanguage: (language: Language) => void
  login: (credentials: Credentials, remember?: boolean) => Promise<void>
  /** X1m-off — signs in with no network, against the PIN hash stored at the last online sign-in. */
  loginOffline: (credentials: Credentials) => Promise<void>
  logout: () => void
}
export const AuthContext = createContext<AuthValue | null>(null)
export function useAuth() {
  const auth = useContext(AuthContext)
  if (!auth) throw new Error('useAuth must be used inside AuthProvider')
  return auth
}
export function roleRedirect(user: User | null, requiredRole?: Role) {
  if (!user) return '/login'
  return requiredRole && user.role !== requiredRole ? rolePaths[user.role] : null
}
