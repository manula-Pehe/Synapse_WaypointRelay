import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth, type Language } from '../../app/auth'
import { api } from '../../lib/api'
import { alertEnabled, setAlertEnabled, type AlertCategory } from '../notifications/data'
import { Button, Card, Feedback, Heading } from './StoreShared'

export function StoreSettings() {
  const { user, token, language, setLanguage, logout } = useAuth()
  const client = useQueryClient()
  const [alerts, setAlerts] = useState<Record<AlertCategory, boolean>>(() => ({
    deliveries: user ? alertEnabled(user, 'deliveries') : true,
    orders: user ? alertEnabled(user, 'orders') : true,
    issues: user ? alertEnabled(user, 'issues') : true,
  }))
  const mutation = useMutation({ mutationFn: (next: Language) => token?.startsWith('demo-') ? Promise.resolve() : api('auth/me', { method: 'PATCH', body: JSON.stringify({ language: next }) }), onSuccess: (_, next) => setLanguage(next) })
  function toggle(category: AlertCategory) {
    if (!user) return
    const next = !alerts[category]
    setAlerts(current => ({ ...current, [category]: next }))
    setAlertEnabled(user, category, next)
    void client.invalidateQueries({ queryKey: ['notifications', user.role, user.id] })
  }
  return <><Heading title="Settings & account" subtitle="Your store preferences" /><Feedback error={mutation.error} success={mutation.isSuccess ? 'Language saved.' : undefined} /><div className="grid gap-4 sm:grid-cols-2">
    <Card><h2 className="text-lg font-bold">Account</h2><p className="mt-3">{user?.name}</p><p className="text-sm text-muted">{user?.outletId} · Store manager</p><div className="mt-4"><Button tone="secondary" onClick={logout}>Sign out</Button></div></Card>
    <Card><h2 className="text-lg font-bold">Language</h2><div className="mt-4 grid gap-2">{([['en','English'],['si','සිංහල'],['ta','தமிழ்']] as const).map(([code,label]) => <label key={code} className="flex min-h-12 items-center gap-3 rounded-lg border border-line px-3"><input type="radio" checked={language === code} onChange={() => mutation.mutate(code)} />{label}</label>)}</div></Card>
    <Card><h2 className="text-lg font-bold">Alerts</h2><p className="mt-2 text-sm text-muted">Choose which updates appear in your notification bell on this device.</p><div className="mt-3 grid gap-2">{(['deliveries','orders','issues'] as const).map(category => <label key={category} className="flex min-h-12 items-center justify-between gap-3 rounded-lg border border-line px-3 capitalize">{category}<input type="checkbox" checked={alerts[category]} onChange={() => toggle(category)} className="size-5" /></label>)}</div><Link to="/store/notifications" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">View notifications →</Link></Card>
    <Card><h2 className="text-lg font-bold">History</h2><p className="mt-2 text-muted">Past deliveries and proof.</p><Link to="/store/history" className="mt-3 inline-flex min-h-12 items-center font-semibold text-brand">Browse history →</Link></Card>
  </div></>
}
