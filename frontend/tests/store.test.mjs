import test from 'node:test'
import assert from 'node:assert/strict'
import { demoStoreApi } from '../src/mocks/store.ts'

test('prepared demo order can be edited, confirmed, and edited until cut-off', async () => {
  const order = await demoStoreApi.edit('demo-1', 28)
  assert.equal(order.units, 28)
  const confirmed = await demoStoreApi.confirm('demo-1')
  assert.equal(confirmed.status, 'CONFIRMED')
  const revised = await demoStoreApi.edit('demo-1', 30)
  assert.equal(revised.units, 30)
  assert.equal(revised.status, 'CONFIRMED')
})

test('receipt is refused before delivery', async () => {
  await assert.rejects(() => demoStoreApi.receipt('demo-2', 18, ''), error => error.code === 'INVALID_STATUS')
})

test('dispatcher reply appears in the store issue thread', async () => {
  const issue = await demoStoreApi.createIssue({ orderId: 'demo-1', type: 'DAMAGED', units: 2, wants: 'REPLACE', note: 'Two cases damaged' })
  await demoStoreApi.dispatchReply(issue.id, 'Replacement arranged')
  const detail = await demoStoreApi.issue(issue.id)
  assert.equal(detail.status, 'ANSWERED')
  assert.equal(detail.messages.at(-1).text, 'Replacement arranged')
})
