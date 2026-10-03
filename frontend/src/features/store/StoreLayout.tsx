import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../app/auth'
import { NotificationBell } from '../notifications/Notifications'
import { storeApi } from './api'
import './store-dashboard.css'

const links = [
  { to: '/store', label: 'Home', icon: 'home', end: true },
  { to: '/store/orders', label: 'Orders', icon: 'orders' },
  { to: '/store/deliveries', label: 'Deliveries', icon: 'truck' },
  { to: '/store/issues', label: 'Issues', icon: 'warning' },
  { to: '/store/settings', label: 'Settings', icon: 'user' },
]

function StoreIcon({ name, size = 20 }: { name: string; size?: number }) {
  return <img src={`/store-icons/${name}.svg`} alt="" width={size} height={size} aria-hidden="true" />
}

function cutoffLabel(now: string, cutOffAt: string, closed: boolean) {
  if (closed) return 'Orders closed · 4:00 PM'
  const minutes = Math.max(0, Math.ceil((new Date(cutOffAt).getTime() - new Date(now).getTime()) / 60_000))
  if (!minutes) return 'Orders close at 4:00 PM'
  return `Orders close in ${Math.floor(minutes / 60)} h ${minutes % 60} min · 4:00 PM`
}

export function StoreLayout() {
  const { user, logout } = useAuth()
  const { pathname } = useLocation()
  const home = useQuery({ queryKey: ['store', 'home'], queryFn: storeApi.home, refetchInterval: 30_000 })
  const isOrders = pathname === '/store/orders'
  const isHome = pathname === '/store'
  const orderList = useQuery({ queryKey: ['store', 'orders'], queryFn: () => storeApi.orders(), enabled: isOrders })
  const upcomingCount = orderList.data?.items.filter(order => !['DELIVERED', 'PARTIAL', 'FAILED', 'CANCELLED'].includes(order.status)).length ?? 0
  const date = home.data ? new Date(home.data.now) : null
  const dateLabel = date?.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' }).replace('Sept', 'Sep') ?? ''
  const timeLabel = date?.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }) ?? ''
  const title = isOrders ? 'Orders' : isHome ? `Good afternoon, ${user?.name?.split(' ')[0] ?? 'Store manager'}` : 'Store manager'
  const subtitle = isOrders ? `Upcoming and past orders for ${user?.outletId ?? 'your outlet'}` : isHome ? `${dateLabel} · ${timeLabel}` : `${user?.outletId ?? ''} · ${dateLabel}`
  const cutoff = home.data ? cutoffLabel(home.data.now, home.data.cutOffAt, home.data.ordersClosed) : 'Orders close at 4:00 PM'

  return <div className="store-shell">
    <aside className="store-sidebar">
      <NavLink to="/store" className="store-brand"><span className="store-brand-mark"><StoreIcon name="brand-box" size={22} /></span><span><small>WAYPOINT</small><strong>Relay</strong></span></NavLink>
      <nav aria-label="Store" className="store-side-links">{links.map(link => <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => `store-side-link${isActive ? ' active' : ''}`}>{({ isActive }) => <><StoreIcon name={link.icon === 'home' && !isActive ? 'home-inactive' : link.icon === 'orders' && isActive ? 'orders-active' : link.icon} />{link.label}</>}</NavLink>)}</nav>
      <div className="store-outlet-card"><strong>{user?.outletId} · Colombo</strong><span>Waypoint Fresh · street · van only</span><span>Delivery window 5:00 – 7:30 AM</span><button onClick={logout}>Sign out</button></div>
    </aside>
    <header className="store-topbar">
      <div className="store-mobile-status"><span>{timeLabel}</span><span className="store-mobile-battery" /></div>
      <div className="store-topbar-inner"><div className="store-page-title"><h1>{title}</h1><p className="store-desktop-subtitle">{subtitle}</p><p className="store-mobile-subtitle">{user?.outletId} · {isOrders ? `${upcomingCount} upcoming` : dateLabel}</p></div><div className="store-topbar-actions"><span className="store-cutoff"><StoreIcon name="clock" size={18} />{cutoff}</span><NotificationBell iconSrc="/store-icons/bell.svg" /><span className="store-account"><span className="store-avatar" />{user?.name}</span></div></div>
    </header>
    <main className="store-main"><Outlet /></main>
    <nav aria-label="Store" className="store-bottom-nav">{links.map(link => <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => `store-bottom-link${isActive ? ' active' : ''}`}>{({ isActive }) => <><StoreIcon name={link.label === 'Settings' ? 'mobile-more' : link.icon === 'home' && !isActive ? 'mobile-home-inactive' : link.icon === 'orders' && isActive ? 'mobile-orders-active' : `mobile-${link.icon}`} size={22} />{link.label === 'Settings' ? 'More' : link.label}</>}</NavLink>)}</nav>
  </div>
}
