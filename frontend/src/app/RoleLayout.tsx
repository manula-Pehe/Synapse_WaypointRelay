import { Navigate, Outlet } from 'react-router-dom'
import { useEffect } from 'react'
import { ThemeSwitch } from '../ui/components'
import { roleRedirect, useAuth, type Role } from './auth'
import { useMessages } from '../i18n/messages'

export function RoleGuard({ role }: { role: Role }) {
  const { user } = useAuth()
  if (role === 'LOADER' && !user) return <Navigate to="/loader/sign-in" replace />
  const redirect = roleRedirect(user, role)
  return redirect ? <Navigate to={redirect} replace /> : <Outlet />
}

export function RoleLayout() {
  const { user } = useAuth()
  useEffect(() => {
    const saved = localStorage.getItem('waypoint.theme')
    document.documentElement.dataset.theme = saved ?? (user?.role === 'DRIVER' ? 'dark' : 'light')
  }, [user?.role])
  if (!user) return null
  return <Outlet />
}
export function WorkspacePlaceholder() {
  const text = useMessages()
  const { user } = useAuth()
  return (
    <section className="rounded-2xl border border-line bg-surface p-6 sm:p-10">
      <h1 className="text-2xl font-semibold">{text.ready}</h1>
      <p className="mt-3 text-muted">{text.placeholder}</p>
      {user?.role === 'DRIVER' && <div className="mt-5"><ThemeSwitch defaultDark /></div>}
    </section>
  )
}
