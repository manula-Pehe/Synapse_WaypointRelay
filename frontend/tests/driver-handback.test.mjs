import { test } from 'node:test'
import assert from 'node:assert/strict'
import { outletLabel } from '../src/features/driver/outlet.ts'
import { t } from '../src/features/driver/i18n.ts'
import { readLanguage, saveLanguage } from '../src/app/session.ts'
import { SYNC_TYPES } from '../src/lib/offline/types.ts'

/**
 * F12 - the stop list in Sinhala and Tamil, and R8r - goods handed back at the depot.
 *
 * The rule under test is that a driver reads a store, not an identifier: the server sends the
 * outlet name with each stop, and everything the driver reads on the stop list goes through t().
 */

/** A stop's outlet as the server sends it (docs/api.md §8, `/api/driver/today`). */
const outlet = (over = {}) => ({
  id: 'OUT017',
  name: 'Battaramulla Mall Store',
  brand: 'Fresh',
  district: 'Colombo',
  dockType: 'mall_bay',
  windowOpen: '09:00',
  windowClose: '18:00',
  ...over,
})

test('a store is shown by its name, not its outlet id', () => {
  assert.equal(outletLabel(outlet()), 'Battaramulla Mall Store')
})

test('the outlet id stands in only when the server sent no name', () => {
  assert.equal(outletLabel(outlet({ name: undefined })), 'OUT017')
  assert.equal(outletLabel(outlet({ name: '   ' })), 'OUT017')
})

test('every string on the stop list is translated, not hard-coded English', () => {
  // A key is a string only in the English table, so this fails the moment a screen adds one for
  // English alone and forgets the other two.
  for (const language of ['si', 'ta']) {
    assert.ok(t(language, 'driver.stops.title').length > 0)
    assert.ok(t(language, 'driver.stops.cases').length > 0)
    assert.ok(t(language, 'driver.stops.done').length > 0)
    assert.ok(t(language, 'driver.stops.arrive').length > 0)
  }
})

test('the stop list strings really differ between the three languages', () => {
  // Guards against a copy-paste that fills Sinhala and Tamil with English and still passes.
  const english = t('en', 'driver.stops.title')
  assert.notEqual(t('si', 'driver.stops.title'), english)
  assert.notEqual(t('ta', 'driver.stops.title'), english)
})

test('the early-arrival note carries the minutes through in any language', () => {
  for (const language of ['en', 'si', 'ta']) {
    const text = t(language, 'driver.stops.minEarly', { count: 50 })
    assert.ok(text.includes('50'), `${language} dropped the minutes: ${text}`)
  }
})

test('the handback screen has its own strings, not the driver signs in text', () => {
  assert.equal(t('en', 'driver.handback.title'), 'Hand back goods')
  assert.equal(t('en', 'driver.tripEnd.handback'), 'Hand back goods')
  assert.ok(t('en', 'driver.handback.nothing').length > 0)
  assert.ok(t('en', 'driver.handback.why').length > 0)
})

test('the handback asks who took responsibility for it (US-11.2)', () => {
  assert.ok(t('en', 'driver.handback.signature').length > 0)
  assert.ok(t('en', 'driver.handback.signatureHint').length > 0)
})

test('goods handed back at the depot is an action the outbox knows how to queue', () => {
  // The server refuses an unknown type, so a handback the phone cannot send would be a silent gap.
  assert.ok(SYNC_TYPES.includes('GOODS_RETURNED'))
})

test('the language a driver chose is still there after the phone reloads', () => {
  // F12 is only worth anything if it survives a reload at the start of a run.
  const store = new Map()
  globalThis.localStorage = {
    getItem: (key) => (store.has(key) ? store.get(key) : null),
    setItem: (key, value) => void store.set(key, String(value)),
  }
  try {
    assert.equal(readLanguage(), 'en')
    saveLanguage('ta')
    assert.equal(readLanguage(), 'ta')
    // Anything that is not a language we ship falls back to English rather than rendering blank.
    store.set('waypoint.language', 'kl')
    assert.equal(readLanguage(), 'en')
  } finally {
    delete globalThis.localStorage
  }
})
