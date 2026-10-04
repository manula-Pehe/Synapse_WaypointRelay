import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth, type Language } from '../../app/auth'
import { api } from '../../lib/api'
import type { AlertCategory } from '../notifications/data'
import { storeApi, type StoreNotificationSettings } from './api'
import { Button, Card, Feedback, Heading } from './StoreShared'
import { storeWindowLabel } from './storeLive'
import './store-settings.css'

export function StoreSettings() {
  const { user, language, setLanguage, logout } = useAuth()
  const client = useQueryClient()
  const alerts = useQuery({ queryKey: ['store', 'notification-settings'], queryFn: storeApi.notificationSettings })
  const outlet = useQuery({ queryKey: ['store', 'outlet', user?.outletId], queryFn: () => storeApi.outlet(user!.outletId!), enabled: !!user?.outletId })
  const alertsMutation = useMutation({ mutationFn: (next: StoreNotificationSettings) => storeApi.saveNotificationSettings(next), onSuccess: async (next) => {
    client.setQueryData(['store', 'notification-settings'], next)
    await client.invalidateQueries({ queryKey: ['notifications', user?.role, user?.id] })
  } })
  const mutation = useMutation({ mutationFn: (next: Language) => api('auth/me', { method: 'PATCH', body: JSON.stringify({ language: next }) }), onSuccess: (_, next) => setLanguage(next) })
  function toggle(category: AlertCategory) {
    if (!alerts.data) return
    alertsMutation.mutate({ ...alerts.data, [category]: !alerts.data[category] })
  }
  return <><Heading title="Settings" subtitle={`${user?.outletId ?? 'Store'} · ${user?.name ?? ''}`} /><Feedback error={mutation.error} success={mutation.isSuccess ? 'Language saved.' : undefined} /><div className="settings-grid">
    <Card><h2 className="text-lg font-bold">Outlet details</h2>{outlet.data ? <dl className="settings-details"><dt>Outlet</dt><dd>{outlet.data.name} · Waypoint {outlet.data.brand}</dd><dt>Depot</dt><dd>{outlet.data.depot}</dd><dt>Delivery window</dt><dd>{storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose)}</dd><dt>Access</dt><dd>{outlet.data.dockType.replaceAll('_',' ')} · {outlet.data.parkingConstraint.replaceAll('_',' ')}</dd></dl> : <p className="mt-2 text-sm text-muted">{outlet.error instanceof Error ? outlet.error.message : 'Loading outlet details…'}</p>}</Card>
    <Card><h2 className="text-lg font-bold">Account</h2><p className="mt-3">{user?.name}</p><p className="text-sm text-muted">{user?.outletId} · Store manager</p><div className="mt-4"><Button tone="secondary" onClick={logout}>Sign out</Button></div></Card>
    <Card><h2 className="text-lg font-bold">Language</h2><div className="settings-language">{([['en','English'],['si','සිංහල'],['ta','தமிழ்']] as const).map(([code,label]) => <button type="button" key={code} aria-pressed={language === code} disabled={mutation.isPending} onClick={() => mutation.mutate(code)}>{label}</button>)}</div><p className="mt-3 text-sm text-muted">Applies to your account on every device.</p></Card>
    <Card><h2 className="text-lg font-bold">Alerts</h2><p className="mt-2 text-sm text-muted">Choose which updates appear in your notification bell.</p><Feedback error={alerts.error ?? alertsMutation.error} />{!alerts.data ? <p className="mt-3 text-sm text-muted">Loading alert settings…</p> : <div className="mt-3 grid gap-2">{(['deliveries','orders','issues'] as const).map(category => <label key={category} className="flex min-h-12 items-center justify-between gap-3 rounded-lg border border-line px-3 capitalize">{category}<input type="checkbox" checked={alerts.data[category]} disabled={alertsMutation.isPending} onChange={() => toggle(category)} className="size-5" /></label>)}</div>}<Link to="/store/notifications" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">View notifications →</Link></Card>
    <Card><h2 className="text-lg font-bold">History</h2><p className="mt-2 text-muted">Past deliveries and proof.</p><Link to="/store/history" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">Browse history →</Link></Card>
  </div></>
}

export function StoreMore() {
  const { user, language, logout } = useAuth()
  const outlet = useQuery({ queryKey: ['store', 'outlet', user?.outletId], queryFn: () => storeApi.outlet(user!.outletId!), enabled: !!user?.outletId })
  return <div className="store-more"><h1>More</h1><p>{user?.name} · {outlet.data?.name ?? user?.outletId}</p><nav aria-label="More options"><Link to="/store/settings"><strong>⌂ Outlet details</strong><span>{outlet.data ? `Window ${storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose)} · ${outlet.data.parkingConstraint.replaceAll('_',' ')}` : 'Store information'} →</span></Link><Link to="/store/settings"><strong>◎ Language</strong><span>{language === 'si' ? 'සිංහල' : language === 'ta' ? 'தமிழ்' : 'English'} →</span></Link><Link to="/store/settings"><strong>♧ Alerts</strong><span>Order, delivery and issue updates →</span></Link><Link to="/store/history"><strong>▤ Delivery history</strong><span>Past runs and receipts →</span></Link></nav><button type="button" onClick={logout}>← Sign out</button></div>
}
