// D1 · Order queue — wires D1u (unconfirmed), D1b (phone-in) and D10 (history) to the API
import { useState } from 'react'
import OrderQueue from '../../../components/OrderQueue'
import AddOrderDrawer from '../../../components/AddOrderDrawer'
import OrderHistoryDrawer from '../../../components/OrderHistoryDrawer'
import { useCloseOrders } from './useCloseOrders'
import { useCloseStatus, useDispatchOrders, useUnconfirmedOutlets } from './useDispatchOrders'
import { useFleet } from './useFleet'
import { useOrderDetail } from './useOrderDetail'
import { useOutlets } from './useOutlets'
import { usePhoneIn } from './usePhoneIn'
import { ErrorNote, LoadingNote } from './QueryNotes'
import { errorMessage } from './errorMessage'
import { formatDateTime, formatRunDate, largestVehicle, toOrderItems, toOutletOptions, toTimelineEvents, toUnconfirmedStores } from './dispatchMappers'

interface Props { runDate: string; depot: string; onCreatePlan: () => void }

export function OrderQueueScreen({ runDate, depot, onCreatePlan }: Props) {
  const orders = useDispatchOrders(runDate, depot)
  const unconfirmed = useUnconfirmedOutlets(runDate, depot)
  const closeStatus = useCloseStatus(runDate, depot)
  const outlets = useOutlets(depot)
  const fleet = useFleet(runDate, depot)
  const closeOrders = useCloseOrders(runDate, depot)
  const phoneIn = usePhoneIn(runDate, depot)
  const [historyOrderId, setHistoryOrderId] = useState<string | null>(null)
  const [phoneInOutletId, setPhoneInOutletId] = useState<string | null>(null)
  const detail = useOrderDetail(runDate, depot, historyOrderId)

  if (orders.isError || unconfirmed.isError || closeStatus.isError || outlets.isError || fleet.isError) {
    return <ErrorNote title="Could not load the order queue" error={orders.error ?? unconfirmed.error ?? closeStatus.error ?? outlets.error ?? fleet.error} />
  }
  if (orders.isPending || unconfirmed.isPending || closeStatus.isPending || outlets.isPending || fleet.isPending) {
    return <LoadingNote label="Loading orders…" />
  }

  const items = toOrderItems(orders.data.items, outlets.data.items, largestVehicle(fleet.data.items))
  const historyItem = items.find(item => item.id === historyOrderId)
  const status = closeStatus.data
  const result = closeOrders.data
  const closeFeedback = closeOrders.isError
    ? { tone: 'danger' as const, text: errorMessage(closeOrders.error) }
    : result ? { tone: 'success' as const, text: `Orders closed: ${result.confirmed} confirmed, ${result.autoConfirmed} auto-confirmed, ${result.notConfirmed} left out.` } : null

  const requestClose = () => {
    if (window.confirm(`Close orders for ${depot} on ${formatRunDate(runDate)}? Unconfirmed chilled, Style and Tech orders will be left out.`)) closeOrders.mutate()
  }

  return <div className="space-y-4">
    {phoneIn.data !== undefined && <p role="status" className="rounded-lg bg-status-delivered-soft p-3 text-sm font-semibold text-status-delivered">✓ {phoneIn.data} phone-in order {phoneIn.data === 1 ? 'line' : 'lines'} created.</p>}
    <OrderQueue
      orders={items}
      unconfirmedStores={toUnconfirmedStores(unconfirmed.data.items)}
      runDateLabel={formatRunDate(runDate)}
      close={{
        closed: status.closed,
        isClosing: closeOrders.isPending,
        statusText: status.closed ? `Orders closed ${formatDateTime(status.closedAt ?? status.cutOffAt)}` : `Orders open until ${formatDateTime(status.cutOffAt)}`,
        feedback: closeFeedback,
      }}
      onCloseOrders={requestClose}
      onAddOrder={() => setPhoneInOutletId('')}
      onEnterByPhone={setPhoneInOutletId}
      onOpenOrder={setHistoryOrderId}
      onCreatePlan={onCreatePlan}
    />
    {phoneInOutletId !== null && <AddOrderDrawer
      outlets={toOutletOptions(outlets.data.items)}
      initialOutletId={phoneInOutletId}
      runDateLabel={formatRunDate(runDate)}
      ordersClosed={status.closed}
      isSubmitting={phoneIn.isPending}
      onSubmit={phoneIn.mutateAsync}
      onClose={() => setPhoneInOutletId(null)}
    />}
    {historyOrderId !== null && <OrderHistoryDrawer
      title={historyItem ? `${historyItem.ref} · ${historyItem.outletId} · ${historyItem.flags.chilled ? 'Chilled' : historyItem.brand} ${historyItem.cases} cases` : 'Order history'}
      status={detail.data?.order.status ?? null}
      events={toTimelineEvents(detail.data?.history ?? [])}
      isLoading={detail.isPending}
      errorMessage={detail.isError ? errorMessage(detail.error) : null}
      onClose={() => setHistoryOrderId(null)}
    />}
  </div>
}
