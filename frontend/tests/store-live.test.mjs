import test from 'node:test'
import assert from 'node:assert/strict'
import { storeCutoffLabel, storeLocalDate, storeTimeLabel } from '../src/features/store/storeLive.ts'

test('store countdown follows server time and closes at the cut-off', () => {
  const cutOffAt = '2026-09-30T16:00:00+05:30'
  assert.equal(storeCutoffLabel(new Date('2026-09-30T08:30:00Z'), cutOffAt, false), 'Orders close in 2 h 0 min · 4:00 PM')
  assert.equal(storeCutoffLabel(new Date('2026-09-30T08:31:00Z'), cutOffAt, false), 'Orders close in 1 h 59 min · 4:00 PM')
  assert.equal(storeCutoffLabel(new Date('2026-09-30T10:30:00Z'), cutOffAt, false), 'Orders closed · 4:00 PM')
})

test('store date and time use Colombo even when UTC is on the previous day', () => {
  const date = new Date('2026-09-30T20:00:00Z')
  assert.equal(storeLocalDate(date), '2026-10-01')
  assert.equal(storeTimeLabel(date), '1:30 AM')
})
