import test from 'node:test'
import assert from 'node:assert/strict'
import { summarizeDistricts } from '../src/features/dispatch/core/networkModel.ts'

test('district network groups recorded trip progress', () => {
  const trips = [
    { district: 'Colombo', stopsDone: 2, stopsTotal: 2, status: 'COMPLETED' },
    { district: 'Colombo', stopsDone: 1, stopsTotal: 3, status: 'ON_THE_WAY' },
    { district: 'Galle', stopsDone: 0, stopsTotal: 1, status: 'PLANNED' },
  ]
  assert.deepEqual(summarizeDistricts(trips), [
    { district: 'Colombo', trips: 2, stops: 5, done: 3, states: { Planned: 0, 'On road': 1, Completed: 1 } },
    { district: 'Galle', trips: 1, stops: 1, done: 0, states: { Planned: 1, 'On road': 0, Completed: 0 } },
  ])
})
