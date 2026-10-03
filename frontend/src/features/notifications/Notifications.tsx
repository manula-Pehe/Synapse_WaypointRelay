import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { rolePaths, useAuth } from '../../app/auth'
import { getNotifications, groupNotifications, markNotificationsRead, type Notification } from './data'
import { notificationMessages, layoutMessages } from './messages'
import { NoticeIcon, type IconName } from './Icons'
import { useMessages } from '../../i18n/messages'

function useNotifications() {
  const { user } = useAuth()
  return useQuery({ queryKey: ['notifications', user?.role, user?.id], queryFn: () => getNotifications(user!), enabled: !!user, refetchInterval: 30_000 })
}
export function NotificationBell({ iconSrc }: { iconSrc?: string } = {}) {
  const { user, language } = useAuth()
  const { data } = useNotifications()
  if (!user) return null
  const unread = data?.filter((item) => !item.read).length ?? 0
  return <Link to={`${rolePaths[user.role]}/notifications`} aria-label={`${notificationMessages[language].title}, ${unread} ${notificationMessages[language].unread}`} className="relative flex min-h-12 min-w-12 items-center justify-center rounded-lg hover:bg-brand-soft">
    {iconSrc ? <img src={iconSrc} alt="" width="24" height="24" /> : <NoticeIcon name="bell" className="size-6" />}
    {unread > 0 && <span aria-hidden="true" className="absolute right-0 top-0 flex min-h-5 min-w-5 items-center justify-center rounded-full bg-danger px-1 text-[10px] font-bold text-white">{unread > 99 ? '99+' : unread}</span>}
  </Link>
}
function StoreNavigation() {
  const { user, language, logout } = useAuth()
  const text = layoutMessages[language]
  const auth = useMessages()
  const links: { label: string; icon: IconName; home?: boolean }[] = [
    { label: text.home, icon: 'home', home: true }, { label: text.orders, icon: 'orders' }, { label: text.deliveries, icon: 'truck' }, { label: text.issues, icon: 'warning' }, { label: text.settings, icon: 'user' },
  ]
  return <>
    <aside className="fixed inset-y-0 left-0 hidden w-[220px] flex-col border-r border-line bg-surface px-3.5 py-5 lg:flex">
      <Link to="/store" className="mb-6 flex min-h-12 items-center gap-2.5"><span className="rounded-lg bg-brand p-2 text-on-brand"><NoticeIcon name="box" /></span><span><span className="block text-[10px] font-bold text-muted">WAYPOINT</span><span className="text-lg font-bold">Relay</span></span></Link>
      <nav aria-label={text.demo} className="space-y-1">{links.map((link) => link.home ? <Link key={link.label} to="/store" className="flex min-h-12 items-center gap-3 px-3 text-sm"><NoticeIcon name={link.icon} />{link.label}</Link> : <button key={link.label} disabled title={text.coming} className="flex min-h-12 w-full items-center gap-3 px-3 text-left text-sm text-muted disabled:cursor-default"><NoticeIcon name={link.icon} />{link.label}</button>)}</nav>
      <div className="mt-auto rounded-xl bg-inset p-3.5 text-xs text-muted"><p className="mb-2 font-semibold text-ink">{user?.outletId} · {user?.name}</p><p>{text.demo}</p><button onClick={logout} className="mt-2 min-h-12 text-brand">{auth.signOut}</button></div>
    </aside>
    <nav aria-label={text.demo} className="fixed inset-x-0 bottom-0 z-10 grid grid-cols-5 border-t border-line bg-surface px-2 pb-[env(safe-area-inset-bottom)] lg:hidden">
      {links.slice(0, 4).map((link) => link.home ? <Link key={link.label} to="/store" className="flex min-h-20 flex-col items-center justify-center gap-1 text-xs text-muted"><NoticeIcon name={link.icon} />{link.label}</Link> : <button key={link.label} disabled title={text.coming} className="flex min-h-20 flex-col items-center justify-center gap-1 text-xs text-muted disabled:cursor-default"><NoticeIcon name={link.icon} />{link.label}</button>)}
      <Link to="/store/notifications" aria-current="page" className="flex min-h-20 flex-col items-center justify-center gap-1 text-xs font-semibold text-brand"><NoticeIcon name="more" />{text.more}</Link>
    </nav>
  </>
}
// S11 / S11m: day sections on desktop, stacked cards and bottom navigation on phone.
// D14: modal side panel. X2 uses a responsive fallback pending its design reference.
export function NotificationsPage() {
  const { user, language } = useAuth()
  const text = notificationMessages[language]
  const labels = layoutMessages[language]
  const [filter, setFilter] = useState('all')
  const [selected, setSelected] = useState<Notification | null>(null)
  const query = useNotifications()
  const client = useQueryClient()
  const navigate = useNavigate()
  const panel = useRef<HTMLDialogElement>(null)
  const detail = useRef<HTMLDialogElement>(null)
  const isDispatch = user?.role === 'DISPATCHER'
  const mutation = useMutation({
    mutationFn: (ids: string[]) => markNotificationsRead(user!, ids),
    onSuccess: () => client.invalidateQueries({ queryKey: ['notifications', user?.role, user?.id] }),
  })
  useEffect(() => {
    const dialog = panel.current
    if (isDispatch) dialog?.showModal()
    const previous = document.body.style.overflow
    if (isDispatch) document.body.style.overflow = 'hidden'
    return () => { dialog?.close(); document.body.style.overflow = previous }
  }, [isDispatch])
  useEffect(() => { if (selected) detail.current?.showModal(); else detail.current?.close() }, [selected])
  if (!user) return null
  const items = query.data ?? []
  const unread = items.filter((item) => !item.read)
  const filtered = items.filter((item) => filter === 'all' || (filter === 'action' ? item.needsAction : isDispatch ? item.severity === filter : item.category === filter))
  const groups = isDispatch ? groupNotifications(filtered).map((group) => ({ key: group.severity, label: group.severity === 'critical' ? labels.critical : text[group.severity], items: group.items })) : ['today', 'yesterday'].map((day) => ({ key: day, label: day === 'today' ? labels.today : labels.yesterday, items: filtered.filter((item) => item.day === day) }))
  const filters = isDispatch ? [['all', `${text.all} ${items.length}`], ...(['critical', 'warning', 'info'] as const).map((severity) => [severity, `${text[severity]} ${items.filter((item) => item.severity === severity).length}`])] : [['all', text.all], ['action', `${labels.needsAction} ${items.filter((item) => item.needsAction).length}`], ['deliveries', labels.deliveries], ['orders', labels.orders], ['issues', labels.issues]]
  function open(item: Notification) { setSelected(item); if (!item.read) mutation.mutate([item.id]) }
  const markAll = <button disabled={!unread.length || mutation.isPending} onClick={() => mutation.mutate(unread.map((item) => item.id))} className="min-h-12 shrink-0 text-xs font-semibold text-brand disabled:cursor-default disabled:opacity-50">{mutation.isPending ? text.saving : text.markAll}</button>
  const content = <>
    <div className={`flex flex-wrap items-center justify-between gap-3 ${isDispatch ? 'mb-3' : 'mb-8 max-lg:mb-4'}`}>
      <div className="flex flex-wrap gap-2" aria-label={text.title}>{filters.map(([value, label]) => <button key={value} aria-pressed={filter === value} onClick={() => setFilter(value)} className={`min-h-12 rounded-full border px-3 text-xs font-medium ${filter === value ? 'border-brand bg-brand text-on-brand' : 'border-line bg-surface text-ink'}`}>{label}</button>)}</div>
      {!isDispatch && markAll}
    </div>
    {mutation.isError && <p role="alert" className="mb-4 text-danger">{text.saveError}</p>}
    {query.isPending ? <p role="status">{text.loading}</p> : query.isError ? <div role="alert"><p>{text.error}</p><button className="min-h-12 text-brand" onClick={() => void query.refetch()}>{text.retry}</button></div> : <>
      {!filtered.length && <div className="py-12 text-center"><h2 className="font-semibold">{text.empty}</h2><p className="mt-2 text-muted">{text.emptyBody}</p></div>}
      {groups.filter((group) => group.items.length).map((group) => <section key={group.key} className={isDispatch ? 'mb-4' : 'mb-3'}>
        <h2 className={`mb-2 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-wide ${!isDispatch ? 'max-lg:hidden text-muted' : group.key === 'critical' ? 'text-danger' : group.key === 'warning' ? 'text-warning' : 'text-brand'}`}>
          {isDispatch && <NoticeIcon name={group.key === 'critical' ? 'critical' : group.key === 'warning' ? 'warning' : 'info'} className="size-3.5" />}{group.label}
        </h2>
        <ul className={isDispatch ? 'space-y-3' : 'space-y-2.5 max-lg:space-y-3'}>{group.items.map((item) => <li key={item.id}>
          <button onClick={() => open(item)} aria-label={`${item.title[language]}. ${item.read ? '' : text.unread + '. '}${labels.markHint}`} className={`flex w-full items-center gap-3 rounded-xl border p-3.5 text-left transition-colors hover:bg-brand-soft ${isDispatch && item.severity === 'critical' ? 'border-danger' : 'border-line'} ${!isDispatch && item.read ? 'bg-canvas max-lg:bg-surface' : 'bg-surface'} ${!isDispatch ? 'max-lg:items-start max-lg:p-4' : ''}`}>
            {!isDispatch && <span className={`flex size-9 shrink-0 items-center justify-center rounded-full ${item.icon === 'check' ? 'bg-success-soft text-success' : item.icon === 'warning' ? 'bg-warning-soft text-warning' : 'bg-brand-soft text-brand'}`}><NoticeIcon name={item.icon} className="size-4.5" /></span>}
            <span className="min-w-0 flex-1"><span className={`block font-bold leading-5 ${isDispatch ? 'text-sm' : 'text-sm max-lg:text-base'}`}>{item.title[language]}</span><span className="mt-1 block text-[13px] leading-5 text-muted">{item.message[language]}</span>{!isDispatch && <span className="mt-1 block text-sm text-muted lg:hidden">{item.day === 'yesterday' ? `${labels.yesterday} ` : ''}{item.time}</span>}</span>
            {!isDispatch && item.needsAction && <span className="hidden min-h-12 items-center rounded-lg bg-brand px-4 text-xs font-semibold text-on-brand lg:flex">{labels.receipt}</span>}
            <span className={`shrink-0 text-xs text-muted ${!isDispatch ? 'max-lg:hidden' : ''}`}>{item.day === 'yesterday' && isDispatch ? labels.yesterday : item.time}</span>
            {isDispatch ? <NoticeIcon name="arrow" className="size-4 shrink-0 text-muted" /> : !item.read && <span className="size-2 shrink-0 rounded-full bg-brand max-lg:hidden" />}
          </button>
        </li>)}</ul>
      </section>)}
    </>}
    <p className="mt-5 text-xs text-muted">{text.demo}</p>
  </>
  return <>
    {isDispatch ? <dialog ref={panel} onCancel={() => navigate(rolePaths[user.role])} onClick={(event) => { if (event.target === panel.current) navigate(rolePaths[user.role]) }} aria-labelledby="notifications-title" className="fixed inset-y-0 left-auto right-0 m-0 h-dvh max-h-none w-full max-w-[460px] border-0 bg-surface p-0 text-ink backdrop:bg-overlay">
      <div className="min-h-full p-6"><header className="mb-9 flex items-center gap-3"><h1 id="notifications-title" className="mr-auto text-xl font-bold">{text.title}</h1>{markAll}<button aria-label={labels.close} onClick={() => navigate(rolePaths[user.role])} className="flex min-h-12 min-w-12 items-center justify-center"><NoticeIcon name="close" /></button></header>{content}</div>
    </dialog> : <div className={`min-h-svh bg-canvas ${user.role === 'STORE_MANAGER' ? 'lg:pl-[220px]' : ''}`}>
      {user.role === 'STORE_MANAGER' && <StoreNavigation />}
      <header className="flex items-center justify-between gap-3 border-b border-line bg-surface px-6 py-3 max-lg:px-4 max-lg:py-6 lg:px-7">
        <div><h1 className="text-xl font-bold max-lg:text-2xl">{text.title}</h1><p aria-live="polite" className="mt-1 text-sm text-muted">{unread.length} {labels.new}</p></div>
        <div className="flex items-center gap-3"><NotificationBell /><span className="hidden size-8 rounded-full bg-inset lg:block" /><span className="hidden text-xs font-semibold lg:block">{user.name}</span></div>
      </header>
      <main className="p-4 pb-28 lg:px-7 lg:py-10">{content}</main>
    </div>}
    <dialog ref={detail} onCancel={() => setSelected(null)} aria-labelledby="notification-detail-title" className="fixed inset-0 m-auto w-[calc(100%-32px)] max-w-lg rounded-2xl border border-line bg-surface p-6 text-ink backdrop:bg-overlay">
      {selected && <><div className="flex items-start justify-between gap-4"><h2 id="notification-detail-title" className="text-lg font-bold">{selected.title[language]}</h2><button onClick={() => setSelected(null)} aria-label={labels.close} className="flex min-h-12 min-w-12 items-center justify-center"><NoticeIcon name="close" /></button></div><p className="mt-3 leading-6 text-muted">{selected.message[language]}</p><p className="mt-5 rounded-lg bg-inset p-4 text-sm leading-6 text-muted">{labels.unavailable}</p>{mutation.isError && <p role="alert" className="mt-3 text-danger">{text.saveError}</p>}<button onClick={() => setSelected(null)} className="mt-5 min-h-12 rounded-lg bg-brand px-5 text-on-brand">{labels.close}</button></>}
    </dialog>
  </>
}
