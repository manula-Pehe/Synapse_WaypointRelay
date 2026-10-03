import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'
import { rolePaths, roleRedirect, useAuth, type Role } from './auth'
import { NotificationBell } from '../features/notifications/Notifications'
import { useMessages } from '../i18n/messages'

export function RoleGuard({ role }: { role: Role }) {
  const { user } = useAuth()
  const redirect = roleRedirect(user, role)
  return redirect ? <Navigate to={redirect} replace /> : <Outlet />
}

export function RoleLayout() {
  const { user, logout } = useAuth()
  const text = useMessages()
  const location = useLocation()
  if (!user) return null
  if (user.role === 'STORE_MANAGER') return <Outlet />
  return (
    <div className="min-h-svh">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-5 py-4 sm:px-8">
          <Link
            to={rolePaths[user.role]}
            className="flex min-h-12 items-center font-bold text-brand"
          >
            Waypoint Relay ↗
          </Link>
          <div className="flex flex-wrap items-center gap-3">
            {user.role === 'DISPATCHER' && <Link to="/dispatch/issues" className="flex min-h-12 items-center rounded-lg px-3 text-sm font-semibold text-brand">Store issues</Link>}
            <NotificationBell />
            <span className="text-sm text-muted">{user.name}</span>
            <button
              onClick={logout}
              className="min-h-16 rounded-lg border border-line px-4 text-sm font-medium"
            >
              {text.signOut}
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-5 py-10 sm:px-8">
        <p className="mb-2 text-sm font-medium text-brand">{text[user.role]}</p>
        {user.role === 'DISPATCHER' && location.pathname === '/dispatch/notifications' && <WorkspacePlaceholder />}
        <Outlet />
      </main>
    </div>
  )
}
export function WorkspacePlaceholder() {
  const text = useMessages()
  return (
    <section className="rounded-2xl border border-line bg-surface p-6 sm:p-10">
      <h1 className="text-2xl font-semibold">{text.ready}</h1>
      <p className="mt-3 text-muted">{text.placeholder}</p>
    </section>
  )
}
