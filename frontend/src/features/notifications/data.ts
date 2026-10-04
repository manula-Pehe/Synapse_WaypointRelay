import { notificationFixtures } from './fixtures'
import type { IconName } from './Icons'
import type { Language, User } from '../../app/auth'
import { storeApi } from '../store/api'
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
export function groupNotifications(items: Notification[]) {
  return severities.map((severity) => ({ severity, items: items.filter((item) => item.severity === severity) }))
}
const readIds = new Map<string, Set<string>>()
const keyFor = (user: User) => `${user.role}:${user.id}`
export type AlertCategory = Notification['category']
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
const localized = (en: string, si: string, ta: string) => ({ en, si, ta })
function storeNotificationDay(at: string, now: Date): Notification['day'] {
  const date = storeLocalDate(new Date(at))
  const today = storeLocalDate(now)
  if (date === today) return 'today'
  const yesterday = storeLocalDate(new Date(now.getTime() - 86_400_000))
  return date === yesterday ? 'yesterday' : 'earlier'
}
async function getStoreNotifications(): Promise<Notification[]> {
  const home = await storeApi.home()
  const [orders, issues, deliveries, settings, readIds] = await Promise.all([
    storeApi.orders(), storeApi.issues(), storeApi.deliveries(storeLocalDate(new Date(home.now))),
    storeApi.notificationSettings(), storeApi.notificationReads(),
  ])
  const now = new Date(home.now)
  const items: (Notification & { at: string })[] = []
  const add = (item: Omit<Notification, 'day' | 'time' | 'read'> & { at: string }) => {
    if (!settings[item.category]) return
    items.push({ ...item, day: storeNotificationDay(item.at, now), time: new Date(item.at).toLocaleTimeString('en-US', { timeZone: 'Asia/Colombo', hour: 'numeric', minute: '2-digit' }), read: readIds.includes(item.id) })
  }
  for (const order of orders.items) {
    const href = `/store/orders/${order.id}`
    if (order.status === 'PREPARED' && !order.autoConfirm && !(home.ordersClosed && order.runDate === home.runDate)) {
      add({ id: `order-action:${order.id}`, at: order.updatedAt, category: 'orders', severity: 'warning', icon: 'warning', needsAction: true, href,
        title: localized(`Order needs confirmation · ${order.ref}`, `ඇණවුම තහවුරු කරන්න · ${order.ref}`, `ஆர்டரை உறுதிசெய்யவும் · ${order.ref}`),
        message: localized(`${order.units} ${order.temp === 'CHILLED' ? 'chilled' : 'ambient'} cases for ${order.runDate}.`, `${order.runDate} සඳහා පෙට්ටි ${order.units}ක්.`, `${order.runDate} அன்று ${order.units} பெட்டிகள்.`) })
    } else if (order.source === 'PHONE_IN' && !order.storeChecked) {
      add({ id: `order-check:${order.id}`, at: order.updatedAt, category: 'orders', severity: 'warning', icon: 'warning', needsAction: true, href,
        title: localized(`Phone order needs checking · ${order.ref}`, `දුරකථන ඇණවුම පරීක්ෂා කරන්න · ${order.ref}`, `தொலைபேசி ஆர்டரைச் சரிபார்க்கவும் · ${order.ref}`),
        message: localized(`${order.units} cases for ${order.runDate}.`, `${order.runDate} සඳහා පෙට්ටි ${order.units}ක්.`, `${order.runDate} அன்று ${order.units} பெட்டிகள்.`) })
    } else if (order.status === 'CANCELLED') {
      add({ id: `order-cancelled:${order.id}`, at: order.updatedAt, category: 'orders', severity: 'info', icon: 'info', href,
        title: localized(`Order cancelled · ${order.ref}`, `ඇණවුම අවලංගුයි · ${order.ref}`, `ஆர்டர் ரத்து செய்யப்பட்டது · ${order.ref}`),
        message: localized(`${order.units} cases for ${order.runDate}.`, `${order.runDate} සඳහා පෙට්ටි ${order.units}ක්.`, `${order.runDate} அன்று ${order.units} பெட்டிகள்.`) })
    }
  }
  for (const delivery of deliveries.items) {
    if (['DELIVERED', 'PARTIAL'].includes(delivery.status) && !delivery.receipt) {
      add({ id: `delivery-receipt:${delivery.orderId}`, at: delivery.delivery?.at ?? home.now, category: 'deliveries', severity: 'warning', icon: 'truck', needsAction: true, href: '/store/deliveries',
        title: localized(`Confirm delivery receipt · ${delivery.orderRef}`, `බෙදාහැරීම තහවුරු කරන්න · ${delivery.orderRef}`, `விநியோகத்தை உறுதிசெய்யவும் · ${delivery.orderRef}`),
        message: localized('Check the cases received and confirm the receipt.', 'ලැබුණු පෙට්ටි පරීක්ෂා කර තහවුරු කරන්න.', 'பெற்ற பெட்டிகளைச் சரிபார்த்து உறுதிசெய்யவும்.') })
    }
    if (delivery.arrival?.changedReason) {
      add({ id: `delivery-arrival:${delivery.orderId}:${delivery.arrival.from}`, at: home.now, category: 'deliveries', severity: 'warning', icon: 'truck', href: '/store/deliveries',
        title: localized(`Arrival changed · ${delivery.orderRef}`, `පැමිණීම වෙනස් විය · ${delivery.orderRef}`, `வருகை மாற்றப்பட்டது · ${delivery.orderRef}`),
        message: localized(delivery.arrival.changedReason, delivery.arrival.changedReason, delivery.arrival.changedReason) })
    }
  }
  for (const issue of issues.items) {
    if (issue.status !== 'ANSWERED') continue
    const at = issue.messages.at(-1)?.createdAt ?? issue.createdAt
    add({ id: `issue-answered:${issue.id}:${at}`, at, category: 'issues', severity: 'info', icon: 'info', href: `/store/issues/${issue.id}`,
      title: localized(`Issue answered · ${issue.ref}`, `ගැටලුවට පිළිතුරු ලැබුණි · ${issue.ref}`, `சிக்கலுக்குப் பதில் வந்தது · ${issue.ref}`),
      message: localized(issue.messages.at(-1)?.text ?? 'Open the issue for details.', issue.messages.at(-1)?.text ?? 'විස්තර බලන්න.', issue.messages.at(-1)?.text ?? 'விவரங்களைப் பார்க்கவும்.') })
  }
  return items.sort((a, b) => Date.parse(b.at) - Date.parse(a.at)).slice(0, 30)
}
export async function getNotifications(user: User): Promise<Notification[]> {
  if (user.role === 'STORE_MANAGER') return getStoreNotifications()
  return notificationFixtures(user.role).map((item) => ({ ...item, read: item.read || reads(user).has(item.id) }))
}
export async function markNotificationsRead(user: User, ids: string[]) {
  if (user.role === 'STORE_MANAGER') return storeApi.markNotificationReads(ids)
  const saved = reads(user)
  ids.forEach((id) => saved.add(id))
  try { localStorage.setItem(`waypoint.notifications.${keyFor(user)}`, JSON.stringify([...saved])) } catch { /* Session memory remains available. */ }
}
