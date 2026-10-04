import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { rolePaths, roleRedirect } from '../src/app/auth.ts'
import { isSession } from '../src/app/session.ts'
import { api, ApiError, configureApi } from '../src/lib/api/index.ts'

const originalFetch = globalThis.fetch
afterEach(() => {
  globalThis.fetch = originalFetch
  configureApi(null, () => {})
})

test('all four roles are redirected away from every other role', () => {
  for (const role of Object.keys(rolePaths)) {
    for (const required of Object.keys(rolePaths)) {
      assert.equal(roleRedirect({ role }, required), role === required ? null : rolePaths[role])
    }
    assert.equal(roleRedirect(null, role), '/login')
  }
})
test('invalid saved sessions are rejected', () => {
  for (const value of [
    null,
    {},
    { token: 'x', user: { role: 'ADMIN' } },
    { token: '', user: {} },
  ]) {
    assert.equal(isSession(value), false)
  }
})
test('client sends bearer token and preserves structured API errors', async () => {
  configureApi('token-1', () => {})
  globalThis.fetch = async (url, init) => {
    assert.equal(url, '/api/store/orders')
    assert.equal(init.headers.get('Authorization'), 'Bearer token-1')
    return new Response(JSON.stringify({ code: 'ORDERS_CLOSED', message: 'Orders closed' }), {
      status: 409,
    })
  }
  await assert.rejects(api('store/orders'), {
    code: 'ORDERS_CLOSED',
    message: 'Orders closed',
    status: 409,
  })
})
test('401 clears the session even if the body is not JSON; public login does not', async () => {
  let signedOut = 0
  configureApi('token-1', () => signedOut++)
  globalThis.fetch = async () => new Response('Unauthorized', { status: 401 })
  await assert.rejects(api('store/orders'), ApiError)
  assert.equal(signedOut, 1)
  await assert.rejects(api('auth/login', { authenticated: false }), ApiError)
  assert.equal(signedOut, 1)
})
test('an old request cannot sign out a newer session', async () => {
  let signedOut = false
  configureApi('old', () => {
    signedOut = true
  })
  globalThis.fetch = async () => {
    configureApi('new', () => {
      signedOut = true
    })
    return new Response('', { status: 401 })
  }
  await assert.rejects(api('store/orders'))
  assert.equal(signedOut, false)
})
test('403 reports No access without logging out; 204 supports empty responses', async () => {
  configureApi('token-1', () => assert.fail('403 must not sign out'))
  globalThis.fetch = async () =>
    new Response(JSON.stringify({ code: 'FORBIDDEN' }), { status: 403 })
  await assert.rejects(api('dispatch/orders'), { code: 'FORBIDDEN', message: 'No access' })
  globalThis.fetch = async () => new Response(null, { status: 204 })
  assert.equal(await api('auth/me'), undefined)
})

test('remember-me uses persistent storage; unchecked uses tab storage; sign-out clears both', async () => {
  const { saveSession, readSession } = await import('../src/app/session.ts')
  const storage = () => {
    const values = new Map()
    return {
      getItem: (key) => values.get(key) ?? null,
      setItem: (key, value) => values.set(key, value),
      removeItem: (key) => values.delete(key),
    }
  }
  const oldLocal = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
  const oldSession = Object.getOwnPropertyDescriptor(globalThis, 'sessionStorage')
  Object.defineProperty(globalThis, 'localStorage', { value: storage(), configurable: true })
  Object.defineProperty(globalThis, 'sessionStorage', { value: storage(), configurable: true })
  try {
    const session = {
      token: 'test',
      user: { id: 'test', name: 'Test', role: 'STORE_MANAGER', language: 'en' },
    }
    saveSession(session, true)
    assert.ok(localStorage.getItem('waypoint.session'))
    assert.equal(sessionStorage.getItem('waypoint.session'), null)
    saveSession(session, false)
    assert.equal(localStorage.getItem('waypoint.session'), null)
    assert.ok(sessionStorage.getItem('waypoint.session'))
    assert.deepEqual(readSession(), session)
    saveSession({ ...session, user: { ...session.user, language: 'si' } })
    assert.equal(localStorage.getItem('waypoint.session'), null)
    assert.equal(readSession().user.language, 'si')
    saveSession(null)
    assert.equal(readSession(), null)
  } finally {
    if (oldLocal) Object.defineProperty(globalThis, 'localStorage', oldLocal)
    else delete globalThis.localStorage
    if (oldSession) Object.defineProperty(globalThis, 'sessionStorage', oldSession)
    else delete globalThis.sessionStorage
  }
})
