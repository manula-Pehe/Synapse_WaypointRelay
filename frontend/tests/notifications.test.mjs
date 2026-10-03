import { test } from 'node:test'
import assert from 'node:assert/strict'
import { getNotifications, markNotificationsRead, groupNotifications } from '../src/features/notifications/data.ts'

test('notifications group critical before warning before info regardless of input order', () => {
  const items = [{ id: 'i', severity: 'info' }, { id: 'c', severity: 'critical' }, { id: 'w', severity: 'warning' }]
  assert.deepEqual(groupNotifications(items).flatMap((group) => group.items.map((item) => item.id)), ['c', 'w', 'i'])
})
test('mark-read survives polling and is isolated by user and role', async () => {
  const user = { id: 'notification-test', role: 'STORE_MANAGER' }
  const other = { id: 'notification-test-other', role: 'STORE_MANAGER' }
  const otherRole = { ...user, role: 'DISPATCHER' }
  const initial = await getNotifications(user)
  assert.equal(initial.filter((item) => !item.read).length, 3)
  await markNotificationsRead(user, [initial[0].id])
  assert.equal((await getNotifications(user)).filter((item) => !item.read).length, 2)
  assert.equal((await getNotifications(other)).filter((item) => !item.read).length, 3)
  assert.equal((await getNotifications(otherRole)).filter((item) => !item.read).length, 8)
  await markNotificationsRead(user, initial.map((item) => item.id))
  assert.ok((await getNotifications(user)).every((item) => item.read))
})

test('role fixtures match the store and dispatcher notification categories', async () => {
  const store = await getNotifications({ id: 'store-fixtures', role: 'STORE_MANAGER' })
  assert.equal(store.length, 6)
  assert.equal(store.filter((item) => item.needsAction).length, 1)
  assert.equal(store.filter((item) => item.day === 'yesterday').length, 3)
  const dispatch = await getNotifications({ id: 'dispatch-fixtures', role: 'DISPATCHER' })
  assert.deepEqual(groupNotifications(dispatch).map((group) => group.items.length), [3, 2, 3])
  assert.deepEqual(await getNotifications({ id: 'driver-fixtures', role: 'DRIVER' }), [])
})
