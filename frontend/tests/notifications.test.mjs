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
    if (url === '/api/store/notifications/settings') return new Response(JSON.stringify({ deliveries: true, orders: true, issues: true }), { status: 200 })
    if (url === '/api/notifications/notice-1/read') return new Response(JSON.stringify({ id: 'notice-1' }), { status: 200 })
    throw new Error(`Unexpected request: ${url}`)
  }
  const user = { id: 'user-1', role: 'STORE_MANAGER' }
  const result = await getNotifications(user)
  assert.equal(result[0].title.en, 'Confirm by 4 PM')
  assert.equal(result[0].category, 'orders')
  assert.equal(result[0].read, false)
  await markNotificationsRead(user, ['notice-1'])
  assert.deepEqual(called, [['/api/notifications', 'GET', 'Bearer session-token'], ['/api/store/notifications/settings', 'GET', 'Bearer session-token'], ['/api/notifications/notice-1/read', 'POST', 'Bearer session-token']])
})

test('store alert choices hide muted categories but retain critical notices', async () => {
  configureApi('session-token', () => {})
  globalThis.fetch = async (url) => {
    if (url === '/api/store/notifications/settings') return new Response(JSON.stringify({ deliveries: true, orders: false, issues: true }), { status: 200 })
    if (url === '/api/notifications') return new Response(JSON.stringify({ items: [
      { id: 'muted', severity: 'INFO', type: 'STORE_ORDER', title: 'Order update', body: '', link: null, createdAt: new Date().toISOString(), readAt: null },
      { id: 'critical', severity: 'CRITICAL', type: 'STORE_ORDER', title: 'Urgent order update', body: '', link: null, createdAt: new Date().toISOString(), readAt: null },
    ], total: 2, unreadCount: 2 }), { status: 200 })
    throw new Error(`Unexpected request: ${url}`)
  }
  const result = await getNotifications({ id: 'user-1', role: 'STORE_MANAGER' })
  assert.deepEqual(result.map(item => item.id), ['critical'])
})

test('delivery problems stay visible as urgent delivery alerts', async () => {
  configureApi('session-token', () => {})
  globalThis.fetch = async (url) => {
    if (url === '/api/store/notifications/settings') return new Response(JSON.stringify({ deliveries: false, orders: true, issues: true }), { status: 200 })
    if (url === '/api/notifications') return new Response(JSON.stringify({ items: [
      { id: 'problem', severity: 'CRITICAL', type: 'DELIVERY_PROBLEM', title: 'Delivery problem', body: 'Driver could not complete it.', link: '/store/deliveries/order-1/problem', createdAt: new Date().toISOString(), readAt: null },
    ], total: 1, unreadCount: 1 }), { status: 200 })
    throw new Error(`Unexpected request: ${url}`)
  }
  const [notice] = await getNotifications({ id: 'user-1', role: 'STORE_MANAGER' })
  assert.equal(notice.category, 'deliveries')
  assert.equal(notice.severity, 'critical')
  assert.equal(notice.icon, 'warning')
  assert.equal(notice.href, '/store/deliveries/order-1/problem')
})
