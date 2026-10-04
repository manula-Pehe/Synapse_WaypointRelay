import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { configureApi } from '../src/lib/api/index.ts'
import { dispatchApi } from '../src/features/dispatch/core/api.ts'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch; configureApi(null, () => {}) })

test('dispatcher requests send the run, depot, bearer token, and contract bodies', async () => {
  const requests = []
  configureApi('dispatcher-token', () => {})
  globalThis.fetch = async (url, init) => {
    requests.push({ url, method: init.method ?? 'GET', token: init.headers.get('Authorization'), body: init.body ? JSON.parse(init.body) : null })
    return new Response('{}', { status: 200 })
  }
  await dispatchApi.orders('2026-10-01', 'Peliyagoda')
  await dispatchApi.closeOrders('2026-10-01', 'Peliyagoda')
  await dispatchApi.phoneIn({ outletId: 'OUT001', runDate: '2026-10-01', temp: 'CHILLED', units: 4, note: '' })
  await dispatchApi.setAvailability('VEH036', '2026-10-01', 'OFF_ROAD', 'Brake issue')
  await dispatchApi.confirmFleet('2026-10-01', 'Peliyagoda')
  assert.deepEqual(requests.map(request => [request.url, request.method]), [
    ['/api/orders?runDate=2026-10-01&depot=Peliyagoda', 'GET'],
    ['/api/dispatch/orders/close', 'POST'],
    ['/api/dispatch/orders/phone-in', 'POST'],
    ['/api/dispatch/fleet/VEH036', 'PUT'],
    ['/api/dispatch/fleet/confirm', 'POST'],
  ])
  assert(requests.every(request => request.token === 'Bearer dispatcher-token'))
  assert.deepEqual(requests[2].body, { outletId: 'OUT001', runDate: '2026-10-01', temp: 'CHILLED', units: 4, note: '' })
  assert.deepEqual(requests[3].body, { runDate: '2026-10-01', status: 'OFF_ROAD', reason: 'Brake issue' })
})

test('planning requests use the existing readiness, create, deferral and publish contract', async () => {
  const requests = []
  configureApi('dispatcher-token', () => {})
  globalThis.fetch = async (url, init) => {
    requests.push({ url, method: init.method ?? 'GET', token: init.headers.get('Authorization'), body: init.body ? JSON.parse(init.body) : null })
    return new Response('{"items":[],"total":0}', { status: init.method === 'POST' && url === '/api/dispatch/plans' ? 201 : 200 })
  }
  await dispatchApi.planReadiness('2026-10-01', 'Peliyagoda')
  await dispatchApi.latestPlan('2026-10-01', 'Peliyagoda')
  await dispatchApi.createPlan('2026-10-01', 'Peliyagoda')
  await dispatchApi.planDeferrals('plan/1')
  await dispatchApi.publishPlan('plan/1')
  assert.deepEqual(requests.map(({ url, method }) => [url, method]), [
    ['/api/dispatch/plans/readiness?runDate=2026-10-01&depot=Peliyagoda', 'GET'],
    ['/api/dispatch/plans?runDate=2026-10-01&depot=Peliyagoda', 'GET'],
    ['/api/dispatch/plans', 'POST'],
    ['/api/dispatch/plans/plan%2F1/deferrals', 'GET'],
    ['/api/dispatch/plans/plan%2F1/publish', 'POST'],
  ])
  assert.deepEqual(requests[2].body, { runDate: '2026-10-01', depot: 'Peliyagoda' })
  assert(requests.every(request => request.token === 'Bearer dispatcher-token'))
})
