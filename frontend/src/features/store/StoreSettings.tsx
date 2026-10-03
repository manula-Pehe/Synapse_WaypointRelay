import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth, type Language } from '../../app/auth'
import { api } from '../../lib/api'
import type { AlertCategory } from '../notifications/data'
import { storeApi, type StoreNotificationSettings } from './api'
import { Button, Card, Feedback, Heading } from './StoreShared'

export function StoreSettings() {
  const { user, token, language, setLanguage, logout } = useAuth()
  const client = useQueryClient()
  const alerts = useQuery({ queryKey: ['store', 'notification-settings'], queryFn: storeApi.notificationSettings })
  const alertsMutation = useMutation({ mutationFn: (next: StoreNotificationSettings) => storeApi.saveNotificationSettings(next), onSuccess: async (next) => {
    client.setQueryData(['store', 'notification-settings'], next)
    await client.invalidateQueries({ queryKey: ['notifications', user?.role, user?.id] })
  } })
  const mutation = useMutation({ mutationFn: (next: Language) => token?.startsWith('demo-') ? Promise.resolve() : api('auth/me', { method: 'PATCH', body: JSON.stringify({ language: next }) }), onSuccess: (_, next) => setLanguage(next) })
  function toggle(category: AlertCategory) {
    if (!alerts.data) return
    alertsMutation.mutate({ ...alerts.data, [category]: !alerts.data[category] })
  }
  return <><Heading title="Settings & account" subtitle="Your store preferences" /><Feedback error={mutation.error} success={mutation.isSuccess ? 'Language saved.' : undefined} /><div className="grid gap-4 sm:grid-cols-2">
    <Card><h2 className="text-lg font-bold">Account</h2><p className="mt-3">{user?.name}</p><p className="text-sm text-muted">{user?.outletId} · Store manager</p><div className="mt-4"><Button tone="secondary" onClick={logout}>Sign out</Button></div></Card>
    <Card><h2 className="text-lg font-bold">Language</h2><div className="mt-4 grid gap-2">{([['en','English'],['si','සිංහල'],['ta','தமிழ்']] as const).map(([code,label]) => <label key={code} className="flex min-h-12 items-center gap-3 rounded-lg border border-line px-3"><input type="radio" checked={language === code} onChange={() => mutation.mutate(code)} />{label}</label>)}</div></Card>
    <Card><h2 className="text-lg font-bold">Alerts</h2><p className="mt-2 text-sm text-muted">Choose which updates appear in your notification bell.</p><Feedback error={alerts.error ?? alertsMutation.error} />{!alerts.data ? <p className="mt-3 text-sm text-muted">Loading alert settings…</p> : <div className="mt-3 grid gap-2">{(['deliveries','orders','issues'] as const).map(category => <label key={category} className="flex min-h-12 items-center justify-between gap-3 rounded-lg border border-line px-3 capitalize">{category}<input type="checkbox" checked={alerts.data[category]} disabled={alertsMutation.isPending} onChange={() => toggle(category)} className="size-5" /></label>)}</div>}<Link to="/store/notifications" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">View notifications →</Link></Card>
    <Card><h2 className="text-lg font-bold">History</h2><p className="mt-2 text-muted">Past deliveries and proof.</p><Link to="/store/history" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">Browse history →</Link></Card>
  </div></>
}
