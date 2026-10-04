import { test, beforeEach, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { checkPinOffline, forgetPin, rememberPin } from '../src/lib/offline/pin.ts'

/**
 * X1m-off - signing in at a depot with no signal.
 *
 * The rule under test is the documented one: the PIN is checked against a hash stored at the
 * last online sign-in, and only that driver's own PIN gets in.
 */

// A minimal localStorage and WebCrypto stand-in. Node 24 has both, but localStorage is not global
// in the test runner, so it is installed per-test and removed after.
const store = new Map()
const localStorageStub = {
  getItem: (key) => (store.has(key) ? store.get(key) : null),
  setItem: (key, value) => void store.set(key, String(value)),
  removeItem: (key) => void store.delete(key),
  clear: () => store.clear(),
}

const CREDENTIAL = {
  staffId: 'DRV-0036',
  userId: 'usr-nuwan',
  name: 'Nuwan',
  vehicleId: 'VEH036',
}

beforeEach(() => {
  store.clear()
  globalThis.localStorage = localStorageStub
})

afterEach(() => {
  delete globalThis.localStorage
})

test('the PIN remembered at the last online sign-in gets the driver in offline', async () => {
  await rememberPin(CREDENTIAL, '3636')

  const result = await checkPinOffline('DRV-0036', '3636')

  assert.equal(result.ok, true)
  assert.equal(result.reason, 'ok')
  assert.equal(result.credential.userId, 'usr-nuwan')
  assert.equal(result.credential.vehicleId, 'VEH036')
})

test('a wrong PIN is refused offline', async () => {
  await rememberPin(CREDENTIAL, '3636')

  const result = await checkPinOffline('DRV-0036', '0000')

  assert.equal(result.ok, false)
  assert.equal(result.reason, 'wrong-pin')
  assert.equal(result.credential, null)
})

test("another driver's staff id cannot use this phone's stored PIN", async () => {
  await rememberPin(CREDENTIAL, '3636')

  // The right PIN, but claimed by a different driver.
  const result = await checkPinOffline('DRV-0099', '3636')

  assert.equal(result.ok, false)
  assert.equal(result.reason, 'wrong-pin')
})

test('with nothing remembered there is no offline sign-in at all', async () => {
  const result = await checkPinOffline('DRV-0036', '3636')

  assert.equal(result.ok, false)
  assert.equal(result.reason, 'no-stored-pin')
})

test('signing out forgets the stored PIN', async () => {
  await rememberPin(CREDENTIAL, '3636')
  forgetPin()

  assert.equal((await checkPinOffline('DRV-0036', '3636')).reason, 'no-stored-pin')
})

test('the PIN itself is never written to storage', async () => {
  await rememberPin(CREDENTIAL, '3636')

  for (const [, value] of store) {
    assert.equal(value.includes('3636'), false, `a stored value leaked the PIN: ${value}`)
  }
})

test('the stored hash is salted, so the same PIN does not produce the same value twice', async () => {
  await rememberPin(CREDENTIAL, '3636')
  const first = store.get('waypoint.driver.pin')

  await rememberPin(CREDENTIAL, '3636')
  const second = store.get('waypoint.driver.pin')

  assert.notEqual(first, second)
  // Both still verify, which is the part that matters.
  assert.equal((await checkPinOffline('DRV-0036', '3636')).ok, true)
})
