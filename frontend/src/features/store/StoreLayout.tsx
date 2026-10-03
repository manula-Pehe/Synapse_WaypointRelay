import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../app/auth'
import { NotificationBell } from '../notifications/Notifications'
import './store-dashboard.css'

const links = [
  { to: '/store', label: 'Home', icon: '⌂', end: true },
  { to: '/store/orders', label: 'Orders', icon: '▤' },
  { to: '/store/deliveries', label: 'Deliveries', icon: '▣' },
  { to: '/store/issues', label: 'Issues', icon: '!' },
  { to: '/store/settings', label: 'Settings', icon: '♙' },
]
export function StoreLayout() {
  const { user, logout } = useAuth()
  return <div className="min-h-svh bg-canvas text-ink lg:pl-48">
    <aside className="fixed inset-y-0 left-0 hidden w-48 flex-col border-r border-line bg-surface p-4 lg:flex">
      <NavLink to="/store" className="mb-7 flex items-center gap-2 text-sm font-bold leading-tight text-ink"><span className="rounded-md bg-brand px-2 py-1.5 text-lg text-white">⬡</span><span><small className="block text-[9px] tracking-widest text-muted">WAYPOINT</small>Relay</span></NavLink>
      <nav aria-label="Store" className="space-y-1">{links.map(link => <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => `flex min-h-12 items-center gap-3 rounded-lg px-3 font-medium ${isActive ? 'bg-brand-soft text-brand' : 'text-muted hover:bg-canvas'}`}><span aria-hidden="true" className="w-6 text-center">{link.icon}</span>{link.label}</NavLink>)}</nav>
      <div className="mt-auto rounded-xl bg-inset p-3"><p className="text-xs font-bold">{user?.outletId} · Colombo</p><p className="mt-1 text-[10px] text-muted">Waypoint Fresh · street · van only</p><p className="text-[10px] text-muted">Delivery window 5:00 – 7:30 AM</p><button onClick={logout} className="mt-2 min-h-10 text-xs font-semibold text-brand">Sign out</button></div>
    </aside>
    <header className="sticky top-0 z-10 flex min-h-16 items-center justify-between border-b border-line bg-surface px-4 sm:px-6"><div><p className="font-bold text-ink lg:hidden">Waypoint Relay</p><p className="font-semibold text-ink lg:text-base">Good afternoon, {user?.name?.split(' ')[0] ?? 'Store manager'}</p><p className="text-xs text-muted">{new Date().toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })} · {user?.outletId}</p></div><div className="flex items-center gap-3"><span className="hidden rounded-full bg-warning-soft px-3 py-1 text-xs font-semibold text-warning sm:inline">◷ Orders close at 4:00 PM</span><NotificationBell /><span className="hidden text-xs font-semibold sm:inline">{user?.name}</span></div></header>
    <main className="mx-auto max-w-7xl px-4 pb-28 pt-6 sm:px-6 lg:pb-12"><Outlet /></main>
    <nav aria-label="Store" className="fixed inset-x-0 bottom-0 z-20 grid grid-cols-5 border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] lg:hidden">{links.map(link => <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => `flex min-h-18 flex-col items-center justify-center gap-1 text-xs ${isActive ? 'font-semibold text-brand' : 'text-muted'}`}><span aria-hidden="true" className="text-xl">{link.icon}</span>{link.label}</NavLink>)}</nav>
  </div>
}
