import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../app/auth'
import { NotificationBell } from '../notifications/Notifications'

const links = [
  { to: '/store', label: 'Home', icon: '⌂', end: true },
  { to: '/store/orders', label: 'Orders', icon: '▤' },
  { to: '/store/deliveries', label: 'Deliveries', icon: '▣' },
  { to: '/store/issues', label: 'Issues', icon: '!' },
  { to: '/store/settings', label: 'More', icon: '☰' },
]
export function StoreLayout() {
  const { user, logout } = useAuth()
  return <div className="min-h-svh bg-canvas text-ink lg:pl-60">
    <aside className="fixed inset-y-0 left-0 hidden w-60 flex-col border-r border-line bg-surface p-5 lg:flex">
      <NavLink to="/store" className="mb-8 text-xl font-bold text-brand">▣ Waypoint Relay</NavLink>
      <nav aria-label="Store" className="space-y-1">{links.map(link => <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => `flex min-h-12 items-center gap-3 rounded-lg px-3 font-medium ${isActive ? 'bg-brand-soft text-brand' : 'text-muted hover:bg-canvas'}`}><span aria-hidden="true" className="w-6 text-center">{link.icon}</span>{link.label}</NavLink>)}</nav>
      <div className="mt-auto rounded-xl bg-canvas p-4"><p className="font-semibold">{user?.name}</p><p className="text-sm text-muted">{user?.outletId}</p><button onClick={logout} className="mt-3 min-h-12 text-sm font-semibold text-brand">Sign out</button></div>
    </aside>
    <header className="sticky top-0 z-10 flex min-h-16 items-center justify-between border-b border-line bg-surface px-4 sm:px-8"><div><p className="font-bold text-brand lg:hidden">Waypoint Relay</p><p className="text-sm text-muted">{user?.outletId} · Store</p></div><NotificationBell /></header>
    <main className="mx-auto max-w-5xl px-4 pb-28 pt-6 sm:px-8 lg:pb-12"><Outlet /></main>
    <nav aria-label="Store" className="fixed inset-x-0 bottom-0 z-20 grid grid-cols-5 border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] lg:hidden">{links.map(link => <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => `flex min-h-18 flex-col items-center justify-center gap-1 text-xs ${isActive ? 'font-semibold text-brand' : 'text-muted'}`}><span aria-hidden="true" className="text-xl">{link.icon}</span>{link.label}</NavLink>)}</nav>
  </div>
}
