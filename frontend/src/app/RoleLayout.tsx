import { Navigate, Outlet } from 'react-router-dom'
import { roleRedirect, useAuth, type Role } from './auth'
import { useMessages } from '../i18n/messages'

export function RoleGuard({ role }: { role: Role }) {
  const { user } = useAuth()
  const redirect = roleRedirect(user, role)
  return redirect ? <Navigate to={redirect} replace /> : <Outlet />
}

export function RoleLayout() {
  const { user } = useAuth()
  if (!user) return null
  return <Outlet />
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
