import test from 'node:test'
import assert from 'node:assert/strict'
import { summarizeDistricts } from '../src/features/dispatch/core/networkModel.ts'

test('district network counts trips and derives progress from order outcomes', () => {
  const plan = { status: 'PUBLISHED', vehicles: [
    { vehicleId: 'VEH001', trips: [{ district: 'Colombo', stops: [{ orderId: 'one' }, { orderId: 'two' }] }] },
    { vehicleId: 'VEH002', trips: [{ district: 'Colombo', stops: [{ orderId: 'three' }] }, { district: 'Galle', stops: [{ orderId: 'four' }] }] },
  ] }
  const orders = [
    { id: 'one', status: 'DELIVERED' }, { id: 'two', status: 'PARTIAL' },
    { id: 'three', status: 'ON_THE_WAY' }, { id: 'four', status: 'PLANNED' },
  ]
  assert.deepEqual(summarizeDistricts(plan, orders), [
    { district: 'Colombo', trips: 2, stops: 3, states: { Draft: 0, Planned: 0, 'On road': 1, Completed: 1 } },
    { district: 'Galle', trips: 1, stops: 1, states: { Draft: 0, Planned: 1, 'On road': 0, Completed: 0 } },
  ])
})
