import { test } from 'node:test'
import assert from 'node:assert/strict'
import { getNotifications, markNotificationsRead, groupNotifications } from '../src/features/notifications/data.ts'
import { storeApi } from '../src/features/store/api.ts'

test('notifications group critical before warning before info regardless of input order', () => {
  const items = [{ id: 'i', severity: 'info' }, { id: 'c', severity: 'critical' }, { id: 'w', severity: 'warning' }]
  assert.deepEqual(groupNotifications(items).flatMap((group) => group.items.map((item) => item.id)), ['c', 'w', 'i'])
})
test('store notifications reflect backend orders, settings, and saved reads', async () => {
  const methods = ['home', 'orders', 'issues', 'deliveries', 'notificationSettings', 'notificationReads', 'markNotificationReads']
  const originals = Object.fromEntries(methods.map((method) => [method, storeApi[method]]))
  const readIds = new Set()
  let settings = { deliveries: true, orders: true, issues: true }
  const user = { id: 'notification-test', role: 'STORE_MANAGER' }
  try {
    storeApi.home = async () => ({ now: '2026-09-30T08:00:00Z', runDate: '2026-10-01', ordersClosed: false })
    storeApi.orders = async () => ({ items: [
      { id: 'one', ref: 'T-001', runDate: '2026-10-01', status: 'PREPARED', autoConfirm: false, units: 12, temp: 'CHILLED', source: 'PREPARED', updatedAt: '2026-09-30T07:00:00Z' },
      { id: 'two', ref: 'T-002', runDate: '2026-10-01', status: 'CANCELLED', autoConfirm: false, units: 8, temp: 'AMBIENT', source: 'MANUAL', updatedAt: '2026-09-30T06:00:00Z' },
    ], total: 2 })
    storeApi.issues = async () => ({ items: [], total: 0 })
    storeApi.deliveries = async () => ({ items: [], total: 0 })
    storeApi.notificationSettings = async () => settings
    storeApi.notificationReads = async () => [...readIds]
    storeApi.markNotificationReads = async (ids) => { ids.forEach((id) => readIds.add(id)) }

    const initial = await getNotifications(user)
    assert.deepEqual(initial.map((item) => item.id), ['order-action:one', 'order-cancelled:two'])
    await markNotificationsRead(user, [initial[0].id])
    assert.deepEqual((await getNotifications(user)).map((item) => item.read), [true, false])
    settings = { ...settings, orders: false }
    assert.deepEqual(await getNotifications(user), [])
    assert.equal((await getNotifications({ id: 'dispatcher', role: 'DISPATCHER' })).filter((item) => !item.read).length, 8)
  } finally {
    for (const method of methods) storeApi[method] = originals[method]
  }
})

test('dispatcher fixtures retain their notification categories', async () => {
  const dispatch = await getNotifications({ id: 'dispatch-fixtures', role: 'DISPATCHER' })
  assert.deepEqual(groupNotifications(dispatch).map((group) => group.items.length), [3, 2, 3])
  assert.deepEqual(await getNotifications({ id: 'driver-fixtures', role: 'DRIVER' }), [])
})
