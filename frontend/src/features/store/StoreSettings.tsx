import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth, type Language } from '../../app/auth'
import { api } from '../../lib/api'
import { storeApi } from './api'
import { Button, Card, Feedback, Heading } from './StoreShared'
import { storeWindowLabel } from './storeLive'
import './store-settings.css'

export function StoreSettings() {
  const { user, language, setLanguage, logout } = useAuth()
  const outlet = useQuery({ queryKey: ['store', 'outlet', user?.outletId], queryFn: () => storeApi.outlet(user!.outletId!), enabled: !!user?.outletId })
  const mutation = useMutation({ mutationFn: (next: Language) => api('auth/me', { method: 'PATCH', body: JSON.stringify({ language: next }) }), onSuccess: (_, next) => setLanguage(next) })
  return <><Heading title="Settings" subtitle={`${user?.outletId ?? 'Store'} · ${user?.name ?? ''}`} /><Feedback error={mutation.error} success={mutation.isSuccess ? 'Language saved.' : undefined} /><div className="settings-grid">
    <Card><h2 className="text-lg font-bold">Outlet details</h2>{outlet.data ? <dl className="settings-details"><dt>Outlet</dt><dd>{outlet.data.name} · Waypoint {outlet.data.brand}</dd><dt>Depot</dt><dd>{outlet.data.depot}</dd><dt>Delivery window</dt><dd>{storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose)}</dd><dt>Access</dt><dd>{outlet.data.dockType.replaceAll('_',' ')} · {outlet.data.parkingConstraint.replaceAll('_',' ')}</dd></dl> : <p className="mt-2 text-sm text-muted">{outlet.error instanceof Error ? outlet.error.message : 'Loading outlet details…'}</p>}</Card>
    <Card><h2 className="text-lg font-bold">Account</h2><p className="mt-3">{user?.name}</p><p className="text-sm text-muted">{user?.outletId} · Store manager</p><div className="mt-4"><Button tone="secondary" onClick={logout}>Sign out</Button></div></Card>
    <Card><h2 className="text-lg font-bold">Language</h2><div className="settings-language">{([['en','English'],['si','සිංහල'],['ta','தமிழ்']] as const).map(([code,label]) => <button type="button" key={code} aria-pressed={language === code} disabled={mutation.isPending} onClick={() => mutation.mutate(code)}>{label}</button>)}</div><p className="mt-3 text-sm text-muted">Applies to your account on every device.</p></Card>
    <Card><h2 className="text-lg font-bold">Alerts</h2><p className="mt-2 text-sm text-muted">Order, delivery, and issue updates appear in your notification bell. Alert preferences are not yet available from the server.</p><Link to="/store/notifications" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">View notifications →</Link></Card>
    <Card><h2 className="text-lg font-bold">History</h2><p className="mt-2 text-muted">Past deliveries and proof.</p><Link to="/store/history" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">Browse history →</Link></Card>
  </div></>
}

export function StoreMore() {
  const { user, language, logout } = useAuth()
  const outlet = useQuery({ queryKey: ['store', 'outlet', user?.outletId], queryFn: () => storeApi.outlet(user!.outletId!), enabled: !!user?.outletId })
  return <div className="store-more"><h1>More</h1><p>{user?.name} · {outlet.data?.name ?? user?.outletId}</p><nav aria-label="More options"><Link to="/store/settings"><strong>⌂ Outlet details</strong><span>{outlet.data ? `Window ${storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose)} · ${outlet.data.parkingConstraint.replaceAll('_',' ')}` : 'Store information'} →</span></Link><Link to="/store/settings"><strong>◎ Language</strong><span>{language === 'si' ? 'සිංහල' : language === 'ta' ? 'தமிழ்' : 'English'} →</span></Link><Link to="/store/settings"><strong>♧ Alerts</strong><span>Order, delivery and issue updates →</span></Link><Link to="/store/history"><strong>▤ Delivery history</strong><span>Past runs and receipts →</span></Link></nav><button type="button" onClick={logout}>← Sign out</button></div>
}
