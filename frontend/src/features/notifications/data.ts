import { notificationFixtures } from './fixtures'
import type { IconName } from './Icons'
import type { Language, User } from '../../app/auth'

export const severities = ['critical', 'warning', 'info'] as const
export type Severity = (typeof severities)[number]
export interface Notification {
  id: string
  severity: Severity
  title: Record<Language, string>
  message: Record<Language, string>
  read: boolean
  category: 'deliveries' | 'orders' | 'issues'
  day: 'today' | 'yesterday'
  time: string
  icon: IconName
  needsAction?: boolean
}
export function groupNotifications(items: Notification[]) {
  return severities.map((severity) => ({ severity, items: items.filter((item) => item.severity === severity) }))
}
const readIds = new Map<string, Set<string>>()
const keyFor = (user: User) => `${user.role}:${user.id}`
export type AlertCategory = Notification['category']
export function alertEnabled(user: User, category: AlertCategory) {
  try { return localStorage.getItem(`waypoint.alerts.${keyFor(user)}.${category}`) !== 'false' }
  catch { return true }
}
export function setAlertEnabled(user: User, category: AlertCategory, enabled: boolean) {
  try { localStorage.setItem(`waypoint.alerts.${keyFor(user)}.${category}`, String(enabled)) } catch { /* Preference remains local to this session. */ }
}
function reads(user: User) {
  const key = keyFor(user)
  if (!readIds.has(key)) {
    try {
      const value: unknown = JSON.parse(localStorage.getItem(`waypoint.notifications.${key}`) ?? '[]')
      readIds.set(key, new Set(Array.isArray(value) ? value.filter((id): id is string => typeof id === 'string') : []))
    } catch { readIds.set(key, new Set()) }
  }
  return readIds.get(key)!
}
// Fictional UI fixtures. Replace this adapter when the notification contract lands.
export async function getNotifications(user: User): Promise<Notification[]> {
  return notificationFixtures(user.role).filter(item => user.role !== 'STORE_MANAGER' || alertEnabled(user, item.category)).map((item) => ({ ...item, read: item.read || reads(user).has(item.id) }))
}
export async function markNotificationsRead(user: User, ids: string[]) {
  const saved = reads(user)
  ids.forEach((id) => saved.add(id))
  try { localStorage.setItem(`waypoint.notifications.${keyFor(user)}`, JSON.stringify([...saved])) } catch { /* Session memory remains available. */ }
}
