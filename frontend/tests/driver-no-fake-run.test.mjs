import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readdirSync, readFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'
import { t } from '../src/features/driver/i18n.ts'

/**
 * Nothing in the driver app is invented.
 *
 * Before the plan is published the server sends no trip, and the app used to answer that with a
 * made-up run - vehicle VEH036 and five invented stops. On a screen it is indistinguishable from a
 * real run, which is the problem: a driver would drive to stores that were never on the plan, and a
 * judge would score invented data as a working offline app. So there is no mock run to fall back on,
 * and these are the tests that keep it that way.
 */

const FEATURE = new URL('../src/features/driver/', import.meta.url).pathname

/** Every source file under the driver feature, mocks included if they ever come back. */
function driverSources(dir = FEATURE) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name)
    if (entry.isDirectory()) return driverSources(path)
    return entry.name.endsWith('.ts') || entry.name.endsWith('.tsx') ? [path] : []
  })
}

test('the driver app has no made-up run left in it', () => {
  // The fallback itself: a trip object built in the app rather than sent by the server.
  for (const file of driverSources()) {
    const source = readFileSync(file, 'utf8')
    assert.ok(
      !/VEH036|mockTrip|DEMO_RUN/.test(source),
      `${file} still carries made-up driver data`,
    )
  }
})

test('the mock trip file is gone, not just unreferenced', () => {
  // Left on disk it is one import away from coming back, and it still reads like real data.
  assert.equal(existsSync(join(FEATURE, 'mocks.ts')), false)
})

test('a driver with no run is told which of the two it is, not shown a spinner forever', () => {
  // The two reasons a real phone hits this: the plan is not out yet, or there is no signal and
  // nothing saved on the handset.
  assert.match(t('en', 'driver.noTrip.waiting'), /plan/i)
  assert.match(t('en', 'driver.noTrip.offline'), /saved run/i)
  assert.equal(t('en', 'driver.noTrip.retry'), 'Try again')
})

test('the no-run screen says all of it in Sinhala and Tamil too', () => {
  for (const language of ['si', 'ta']) {
    for (const key of [
      'driver.noTrip.waiting',
      'driver.noTrip.offline',
      'driver.noTrip.retry',
    ]) {
      const text = t(language, key)
      assert.ok(text.length > 0, `${language} is missing ${key}`)
      assert.notEqual(text, t('en', key), `${language} left ${key} in English`)
    }
  }
})
