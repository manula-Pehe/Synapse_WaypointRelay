import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth, type Language } from '../../app/auth'
import { api } from '../../lib/api'
import type { AlertCategory } from '../notifications/data'
import { storeApi, type StoreNotificationSettings } from './api'
import { Button, Card, Feedback, Heading } from './StoreShared'
import { storeWindowLabel } from './storeLive'
import { useStoreMessages } from './storeMessages'
import './store-settings.css'

export function StoreSettings() {
  const { user, language, setLanguage, logout } = useAuth()
  const text = useStoreMessages()
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
  return <><Heading title={text.settings} subtitle={`${user?.outletId ?? text.store} · ${user?.name ?? ''}`} /><Feedback error={mutation.error} success={mutation.isSuccess ? text.languageSaved : undefined} /><div className="settings-grid">
    <Card><h2 className="text-lg font-bold">{text.outletDetails}</h2>{outlet.data ? <dl className="settings-details"><dt>{text.outlet}</dt><dd>{outlet.data.name} · Waypoint {outlet.data.brand}</dd><dt>{text.depot}</dt><dd>{outlet.data.depot}</dd><dt>{text.deliveryWindow}</dt><dd>{storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose)}</dd><dt>{text.access}</dt><dd>{outlet.data.dockType.replaceAll('_',' ')} · {outlet.data.parkingConstraint.replaceAll('_',' ')}</dd></dl> : <p className="mt-2 text-sm text-muted">{outlet.error instanceof Error ? outlet.error.message : text.loadingOutlet}</p>}</Card>
    <Card><h2 className="text-lg font-bold">{text.account}</h2><p className="mt-3">{user?.name}</p><p className="text-sm text-muted">{user?.outletId} · {text.storeManager}</p><div className="mt-4"><Button tone="secondary" onClick={logout}>{text.signOut}</Button></div></Card>
    <Card><h2 className="text-lg font-bold">{text.language}</h2><div className="settings-language">{([['en','English'],['si','සිංහල'],['ta','தமிழ்']] as const).map(([code,label]) => <button type="button" key={code} lang={code} aria-pressed={language === code} disabled={mutation.isPending} onClick={() => mutation.mutate(code)}>{label}</button>)}</div><p className="mt-3 text-sm text-muted">{text.appliesEverywhere}</p></Card>
    <Card><h2 className="text-lg font-bold">{text.alerts}</h2><p className="mt-2 text-sm text-muted">{text.alertsHelp}</p><Feedback error={alerts.error ?? alertsMutation.error} />{!alerts.data ? <p className="mt-3 text-sm text-muted">{text.loadingAlerts}</p> : <div className="mt-3 grid gap-2">{(['deliveries','orders','issues'] as const).map(category => <label key={category} className="flex min-h-12 items-center justify-between gap-3 rounded-lg border border-line px-3"><span>{text[`${category}Alert`]}</span><input type="checkbox" checked={alerts.data[category]} disabled={alertsMutation.isPending} onChange={() => toggle(category)} className="size-5" /></label>)}</div>}<Link to="/store/notifications" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">{text.viewNotifications} →</Link></Card>
    <Card><h2 className="text-lg font-bold">{text.history}</h2><p className="mt-2 text-muted">{text.historyHelp}</p><Link to="/store/history" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">{text.browseHistory} →</Link></Card>
  </div></>
}

export function StoreMore() {
  const { user, language, logout } = useAuth()
  const text = useStoreMessages()
  const outlet = useQuery({ queryKey: ['store', 'outlet', user?.outletId], queryFn: () => storeApi.outlet(user!.outletId!), enabled: !!user?.outletId })
  return <div className="store-more"><h1>{text.more}</h1><p>{user?.name} · {outlet.data?.name ?? user?.outletId}</p><nav aria-label={text.moreOptions}><Link to="/store/settings"><strong>⌂ {text.outletDetails}</strong><span>{outlet.data ? `${text.window} ${storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose)} · ${outlet.data.parkingConstraint.replaceAll('_',' ')}` : text.storeInformation} →</span></Link><Link to="/store/settings"><strong>◎ {text.language}</strong><span>{language === 'si' ? 'සිංහල' : language === 'ta' ? 'தமிழ்' : 'English'} →</span></Link><Link to="/store/settings"><strong>♧ {text.alerts}</strong><span>{text.alertsSummary} →</span></Link><Link to="/store/history"><strong>▤ {text.history}</strong><span>{text.historySummary} →</span></Link></nav><button type="button" onClick={logout}>← {text.signOut}</button></div>
}
