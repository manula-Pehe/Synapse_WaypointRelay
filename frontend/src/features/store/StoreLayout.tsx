import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../app/auth'
import { NotificationBell } from '../notifications/Notifications'
import { storeApi } from './api'
import { storeCutoffLabel, storeDateLabel, storeGreeting, storeTimeLabel, storeWindowLabel, useStoreLiveNow } from './storeLive'
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

export function StoreLayout() {
  const { user, logout } = useAuth()
  const { pathname } = useLocation()
  const home = useQuery({ queryKey: ['store', 'home'], queryFn: storeApi.home, refetchInterval: 15_000, refetchOnWindowFocus: 'always', refetchOnReconnect: 'always' })
  const outlet = useQuery({ queryKey: ['store', 'outlet', user?.outletId], queryFn: () => storeApi.outlet(user!.outletId!), enabled: !!user?.outletId, staleTime: 60_000 })
  const isOrders = pathname === '/store/orders'
  const isReview = pathname === '/store/orders/review'
  const isNewOrder = pathname === '/store/orders/new'
  const isHome = pathname === '/store'
  const orderList = useQuery({ queryKey: ['store', 'orders'], queryFn: () => storeApi.orders(), enabled: isOrders, refetchInterval: 15_000, refetchOnWindowFocus: 'always', refetchOnReconnect: 'always' })
  const upcomingCount = orderList.data?.items.filter(order => !['DELIVERED', 'PARTIAL', 'FAILED', 'CANCELLED'].includes(order.status)).length ?? 0
  const now = useStoreLiveNow(home.data?.now, home.dataUpdatedAt)
  const dateLabel = now ? storeDateLabel(now) : ''
  const timeLabel = now ? storeTimeLabel(now) : ''
  const title = isNewOrder ? 'New order' : isReview ? `Review and confirm · ${home.data?.runDate ? storeDateLabel(new Date(`${home.data.runDate}T12:00:00+05:30`)) : ''}` : isOrders ? 'Orders' : isHome ? `${now ? storeGreeting(now) : 'Hello'}, ${user?.name?.split(' ')[0] ?? 'Store manager'}` : 'Store manager'
  const subtitle = isNewOrder ? 'For an extra delivery, or a day you do not usually get one' : isReview ? 'Review your quantities before the cut-off' : isOrders ? `Upcoming and past orders for ${user?.outletId ?? 'your outlet'}` : isHome ? `${dateLabel} · ${timeLabel}` : `${user?.outletId ?? ''} · ${dateLabel}`
  const cutoff = home.data && now ? storeCutoffLabel(now, home.data.cutOffAt, home.data.ordersClosed) : 'Loading cut-off…'

  return <div className="store-shell">
    <aside className="store-sidebar">
      <NavLink to="/store" className="store-brand"><span className="store-brand-mark"><StoreIcon name="brand-box" size={22} /></span><span><small>WAYPOINT</small><strong>Relay</strong></span></NavLink>
      <nav aria-label="Store" className="store-side-links">{links.map(link => <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => `store-side-link${isActive ? ' active' : ''}`}>{({ isActive }) => <><StoreIcon name={link.icon === 'home' && !isActive ? 'home-inactive' : link.icon === 'orders' && isActive ? 'orders-active' : link.icon} />{link.label}</>}</NavLink>)}</nav>
      <div className="store-outlet-card"><strong>{outlet.data?.name ?? user?.outletId}</strong>{outlet.data && <><span>Waypoint {outlet.data.brand} · {outlet.data.dockType.replaceAll('_', ' ')} · {outlet.data.parkingConstraint.replaceAll('_', ' ')}</span><span>Delivery window {storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose)}</span></>}<button onClick={logout}>Sign out</button></div>
    </aside>
    <header className="store-topbar">
      <div className="store-topbar-inner"><div className="store-page-title"><h1>{title}</h1><p className="store-desktop-subtitle">{subtitle}</p><p className="store-mobile-subtitle">{isNewOrder ? 'Extra or one-off delivery' : isReview ? 'Review quantities' : `${user?.outletId} · ${isOrders ? `${upcomingCount} upcoming` : dateLabel}`}</p></div><div className="store-topbar-actions"><span className="store-cutoff"><StoreIcon name="clock" size={18} />{cutoff}</span><NotificationBell iconSrc="/store-icons/bell.svg" /><span className="store-account"><span className="store-avatar" />{user?.name}</span></div></div>
    </header>
    <main className="store-main"><Outlet /></main>
    <nav aria-label="Store" className="store-bottom-nav">{links.map(link => <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => `store-bottom-link${isActive ? ' active' : ''}`}>{({ isActive }) => <><StoreIcon name={link.label === 'Settings' ? 'mobile-more' : link.icon === 'home' && !isActive ? 'mobile-home-inactive' : link.icon === 'orders' && isActive ? 'mobile-orders-active' : `mobile-${link.icon}`} size={22} />{link.label === 'Settings' ? 'More' : link.label}</>}</NavLink>)}</nav>
  </div>
}
