import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { getNotifications, markNotificationsRead, groupNotifications } from '../src/features/notifications/data.ts'
import { configureApi } from '../src/lib/api/index.ts'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch; configureApi(null, () => {}) })

test('notifications group critical before warning before info', () => {
  const items = [{ id: 'i', severity: 'info' }, { id: 'c', severity: 'critical' }, { id: 'w', severity: 'warning' }]
  assert.deepEqual(groupNotifications(items).flatMap(group => group.items.map(item => item.id)), ['c', 'w', 'i'])
})

test('notifications use the signed-in user API and persist read state on the server', async () => {
  configureApi('session-token', () => {})
  const called = []
  globalThis.fetch = async (url, options) => {
    called.push([url, options.method ?? 'GET', options.headers.get('Authorization')])
    if (url === '/api/notifications') return new Response(JSON.stringify({ items: [{ id: 'notice-1', severity: 'WARNING', type: 'STORE_REMINDER', title: 'Confirm by 4 PM', body: 'One order is ready', link: '/store/orders', createdAt: new Date().toISOString(), readAt: null }], total: 1, unreadCount: 1 }), { status: 200 })
    if (url === '/api/notifications/notice-1/read') return new Response(JSON.stringify({ id: 'notice-1' }), { status: 200 })
    throw new Error(`Unexpected request: ${url}`)
  }
  const user = { id: 'user-1', role: 'STORE_MANAGER' }
  const result = await getNotifications(user)
  assert.equal(result[0].title.en, 'Confirm by 4 PM')
  assert.equal(result[0].category, 'orders')
  assert.equal(result[0].read, false)
  await markNotificationsRead(user, ['notice-1'])
  assert.deepEqual(called, [['/api/notifications', 'GET', 'Bearer session-token'], ['/api/notifications/notice-1/read', 'POST', 'Bearer session-token']])
})
