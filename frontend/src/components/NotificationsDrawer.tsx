import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { dispatchApi } from '../features/dispatch/core/api'

export default function NotificationsDrawer({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
  const navigate = useNavigate()
  const client = useQueryClient()
  const notices = useQuery({ queryKey: ['dispatch-notifications'], queryFn: dispatchApi.notifications, refetchInterval: 30_000, enabled: isOpen })
  const refresh = () => client.invalidateQueries({ queryKey: ['dispatch-notifications'] })
  const markRead = useMutation({ mutationFn: dispatchApi.markNotificationRead, onSuccess: refresh })
  const markAll = useMutation({ mutationFn: dispatchApi.markAllNotificationsRead, onSuccess: refresh })
  if (!isOpen) return null
  return <div className="fixed inset-0 z-50 bg-slate-900/40" role="dialog" aria-label="Notifications">
    <button className="absolute inset-0 cursor-default" aria-label="Close notifications" onClick={onClose} />
    <div className="absolute inset-y-0 right-0 w-full max-w-md overflow-auto bg-white p-6 shadow-2xl">
      <div className="flex items-center justify-between"><h2 className="text-lg font-bold">Notifications</h2><button className="min-h-10 min-w-10" onClick={onClose}>Close</button></div>
      <div className="mt-3 flex items-center justify-between text-sm"><span>{notices.data?.unreadCount ?? 0} unread</span><button className="font-semibold text-blue-700 disabled:opacity-50" disabled={!notices.data?.unreadCount || markAll.isPending} onClick={() => markAll.mutate()}>Mark all read</button></div>
      {notices.isPending && <p className="mt-5">Loading notifications…</p>}
      {notices.error && <p role="alert" className="mt-5 text-red-700">{notices.error.message}</p>}
      {(markRead.error || markAll.error) && <p role="alert" className="mt-3 text-red-700">{(markRead.error || markAll.error)?.message}</p>}
      {notices.data?.items.length === 0 && <p className="mt-5 text-slate-600">No notifications.</p>}
      <div className="mt-5 space-y-3">{notices.data?.items.map(notice => <div key={notice.id} className={`rounded-xl border p-4 ${notice.readAt ? 'border-slate-200' : 'border-blue-200 bg-blue-50/50'}`}><div className="flex items-center justify-between gap-2"><strong className="text-sm">{notice.title}</strong><span className="text-xs text-slate-500">{notice.severity}</span></div><p className="mt-1 text-sm text-slate-700">{notice.body}</p><p className="mt-1 text-xs text-slate-500">{notice.createdAt}</p><div className="mt-2 flex gap-4">{notice.link?.startsWith('/') && <button className="text-sm font-semibold text-blue-700" onClick={() => { if (!notice.readAt) markRead.mutate(notice.id); onClose(); navigate(notice.link!) }}>Open</button>}{!notice.readAt && <button className="text-sm text-slate-600 underline" disabled={markRead.isPending} onClick={() => markRead.mutate(notice.id)}>Mark read</button>}</div></div>)}</div>
    </div>
  </div>
}
