import { api } from '../../lib/api'
import type { IconName } from './Icons'
import type { Language, User } from '../../app/auth'
import { storeLocalDate } from '../store/storeLive'

export const severities = ['critical', 'warning', 'info'] as const
export type Severity = (typeof severities)[number]
export interface Notification {
  id: string
  severity: Severity
  title: Record<Language, string>
  message: Record<Language, string>
  read: boolean
  category: 'deliveries' | 'orders' | 'issues'
  day: 'today' | 'yesterday' | 'earlier'
  time: string
  icon: IconName
  needsAction?: boolean
  href?: string
}
interface ApiNotification { id: string; severity: string; type: string; title: string; body: string; link: string | null; createdAt: string; readAt: string | null }
interface ApiNotificationList { items: ApiNotification[]; total: number; unreadCount: number }

export function groupNotifications(items: Notification[]) {
  return severities.map(severity => ({ severity, items: items.filter(item => item.severity === severity) }))
}
const allLanguages = (value: string): Record<Language, string> => ({ en: value, si: value, ta: value })
function category(type: string): Notification['category'] {
  const key = type.toLowerCase()
  return key.includes('issue') ? 'issues' : key.includes('delivery') || key.includes('arrival') || key.includes('driver') || key.includes('receipt') ? 'deliveries' : 'orders'
}
function day(at: string): Notification['day'] {
  const today = storeLocalDate(new Date())
  const date = storeLocalDate(new Date(at))
  if (date === today) return 'today'
  return date === storeLocalDate(new Date(Date.now() - 86_400_000)) ? 'yesterday' : 'earlier'
}
function mapNotification(item: ApiNotification): Notification {
  const severity = item.severity.toLowerCase() as Severity
  const kind = category(item.type)
  return { id: item.id, severity: severities.includes(severity) ? severity : 'info', title: allLanguages(item.title), message: allLanguages(item.body), read: !!item.readAt,
    category: kind, day: day(item.createdAt), time: new Date(item.createdAt).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }),
    icon: kind === 'deliveries' ? 'truck' : severity === 'critical' || severity === 'warning' ? 'warning' : 'info',
    needsAction: severity === 'critical' || severity === 'warning', href: item.link?.startsWith('/') && !item.link.startsWith('//') ? item.link : undefined }
}
export async function getNotifications(_user: User): Promise<Notification[]> {
  void _user
  const result = await api<ApiNotificationList>('notifications')
  return result.items.map(mapNotification)
}
export async function markNotificationsRead(_user: User, ids: string[]) {
  await Promise.all(ids.map(id => api(`notifications/${encodeURIComponent(id)}/read`, { method: 'POST' })))
}
