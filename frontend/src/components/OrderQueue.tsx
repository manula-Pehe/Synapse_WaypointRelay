// D1 · Order queue  (D1u unconfirmed stores · D1b phone-in entry point · D10 history entry point)
import React, { useState } from 'react';
import { Badge, Button, EmptyState } from '../ui/components';

export interface OrderItem {
  id: string;
  ref: string;
  outletId: string;
  outlet: string;
  brand: string;
  cases: number;
  kg: number;
  volumeM3: number;
  window: string;
  flags: {
    chilled: boolean;
    vanOnly: boolean;
    largerThanVehicle: boolean;
    waitedDays: number | null;
    skippedYesterday: boolean;
  };
}

export interface UnconfirmedStore {
  id: string;
  outlet: string;
  preparedOrder: string;
  phone: string | null;
}

export interface CloseControl {
  closed: boolean;
  statusText: string;
  isClosing: boolean;
  feedback: { tone: 'success' | 'danger'; text: string } | null;
}

export interface OrderQueueProps {
  orders: OrderItem[];
  unconfirmedStores: UnconfirmedStore[];
  runDateLabel: string;
  close: CloseControl;
  onCloseOrders: () => void;
  onAddOrder: () => void;
  onEnterByPhone: (outletId: string) => void;
  onOpenOrder: (orderId: string) => void;
  onCreatePlan: () => void;
}

type QueueView = 'all' | 'not-confirmed';

interface OrderCategory {
  id: string;
  label: string;
  matches: (order: OrderItem) => boolean;
}

function buildCategories(orders: OrderItem[]): OrderCategory[] {
  const brands = [...new Set(orders.map((order) => order.brand))].sort();
  const brandCategories = brands.map((brand): OrderCategory => ({
    id: `brand-${brand}`,
    label: brand,
    matches: (order) => order.brand === brand,
  }));
  return [
    { id: 'all', label: 'All', matches: () => true },
    ...brandCategories,
    { id: 'chilled', label: 'Chilled', matches: (order) => order.flags.chilled },
    { id: 'van-only', label: 'Van only', matches: (order) => order.flags.vanOnly },
    { id: 'skipped', label: 'Skipped before', matches: (order) => order.flags.skippedYesterday },
  ];
}

const chip = 'min-h-10 rounded-full border px-3.5 py-1.5 text-xs font-medium transition-all';
const chipIdle = 'border-line bg-surface text-ink hover:bg-inset';
const chipActive = 'border-brand bg-brand-soft font-semibold text-brand';
const card = 'rounded-xl border border-line bg-surface p-5';
const th = 'whitespace-nowrap px-5 py-3.5';
const td = 'whitespace-nowrap px-5 py-4';

function StatCard({ title, value, note, tone = 'text-ink' }: { title: string; value: React.ReactNode; note: string; tone?: string }) {
  return (
    <div className={`${card} flex flex-col justify-between`}>
      <div>
        <span className="text-[11px] font-bold uppercase tracking-wider text-muted">{title}</span>
        <div className={`mt-2 text-3xl font-extrabold tracking-tight ${tone}`}>{value}</div>
      </div>
      <p className="mt-3 text-xs text-muted">{note}</p>
    </div>
  );
}

function OrderFlags({ flags }: { flags: OrderItem['flags'] }) {
  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {flags.chilled && <Badge status="chilled" size="s" icon="❄">Chilled</Badge>}
      {flags.vanOnly && <Badge status="offline" size="s" icon="▭">Van only</Badge>}
      {flags.largerThanVehicle && <Badge status="failed" size="s" icon="⊘">Larger than any vehicle</Badge>}
      {flags.waitedDays !== null && <Badge status="deferred" size="s" icon="◷">Waited {flags.waitedDays} {flags.waitedDays === 1 ? 'day' : 'days'}</Badge>}
      {flags.skippedYesterday && <Badge status="deferred" size="s" icon="↷">Skipped yesterday</Badge>}
    </div>
  );
}

function UnconfirmedBanner({ count, runDateLabel }: { count: number; runDateLabel: string }) {
  if (count === 0) {
    return (
      <div role="status" className="flex items-start gap-3.5 rounded-xl border border-status-delivered bg-status-delivered-soft p-5 text-status-delivered">
        <span aria-hidden="true">✓</span>
        <h2 className="text-base font-bold">All stores have confirmed their orders for {runDateLabel}</h2>
      </div>
    );
  }
  return (
    <div className="flex items-start gap-3.5 rounded-xl border border-status-risk bg-status-risk-soft p-5 text-status-risk">
      <span aria-hidden="true">⚠</span>
      <div>
        <h2 className="text-base font-bold">{count} {count === 1 ? 'store hasn\'t' : 'stores haven\'t'} confirmed their order for {runDateLabel}</h2>
        <p className="mt-1 text-xs sm:text-sm">
          Unconfirmed chilled, Style and Tech orders are not planned once orders close — call them, or enter the order for them if they tell you by phone.
        </p>
      </div>
    </div>
  );
}

function StatsRow({ orders }: { orders: OrderItem[] }) {
  const chilled = orders.filter((order) => order.flags.chilled);
  const chilledVolume = chilled.reduce((sum, order) => sum + order.volumeM3, 0);
  const vanOnly = orders.filter((order) => order.flags.vanOnly);
  const brandSummary = [...new Set(orders.map((order) => order.brand))].sort()
    .map((brand) => `${brand} ${orders.filter((order) => order.brand === brand).length}`).join(' · ');
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <StatCard title="Confirmed orders" value={orders.length} note={brandSummary || 'No orders yet'} />
      <StatCard title="Chilled" tone="text-status-chilled" value={<>{chilled.length} · {chilledVolume.toFixed(0)} <span className="text-2xl font-bold">m³</span></>} note="Needs a fridge vehicle" />
      <StatCard title="Van-only outlets" value={<>{vanOnly.length} <span className="text-2xl font-bold">orders</span></>} note={`${vanOnly.filter((order) => order.flags.chilled).length} chilled`} />
      <StatCard title="Carried over" tone="text-status-deferred" value={orders.filter((order) => order.flags.skippedYesterday).length} note="Skipped before · priority raised" />
    </div>
  );
}

function UnconfirmedTable({ stores, onEnterByPhone }: { stores: UnconfirmedStore[]; onEnterByPhone: (outletId: string) => void }) {
  if (stores.length === 0) return <EmptyState title="Nothing to chase" description="Every store has confirmed its order." />;
  return (
    <div className="overflow-hidden rounded-xl border border-line bg-surface">
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr className="border-b border-line bg-inset text-xs font-semibold uppercase tracking-wider text-muted">
              <th scope="col" className={th}>Outlet</th>
              <th scope="col" className={th}>Prepared order</th>
              <th scope="col" className={th}>Contact</th>
              <th scope="col" className={th}>Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line text-ink">
            {stores.map((store) => (
              <tr key={store.id} className="hover:bg-inset">
                <td className={`${td} font-bold`}>{store.outlet}</td>
                <td className={`${td} text-muted`}>{store.preparedOrder}</td>
                <td className={`${td} text-muted`}>
                  {store.phone ? <a className="font-semibold text-brand underline" href={`tel:${store.phone.replace(/[^+\d]/g, '')}`}>Call {store.phone}</a> : 'No phone number on file'}
                </td>
                <td className={td}>
                  <Button tone="secondary" size="s" onClick={() => onEnterByPhone(store.id)}>☎ Enter by phone</Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function OrdersTable({ orders, onOpenOrder }: { orders: OrderItem[]; onOpenOrder: (orderId: string) => void }) {
  if (orders.length === 0) return <EmptyState title="No orders here" description="No confirmed orders match this filter for the selected run." />;
  return (
    <div className="overflow-hidden rounded-xl border border-line bg-surface">
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr className="border-b border-line bg-inset text-xs font-semibold text-muted">
              <th scope="col" className={th}>Order</th>
              <th scope="col" className={th}>Outlet</th>
              <th scope="col" className={th}>Brand</th>
              <th scope="col" className={`${th} text-right`}>Cases</th>
              <th scope="col" className={`${th} text-right`}>kg</th>
              <th scope="col" className={`${th} text-right`}>m³</th>
              <th scope="col" className={th}>Window</th>
              <th scope="col" className={th}>Flags</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line text-ink">
            {orders.map((order) => (
              <tr key={order.id} className="hover:bg-inset">
                <td className={td}>
                  <button type="button" onClick={() => onOpenOrder(order.id)} aria-label={`Open history of order ${order.ref}`} className="min-h-10 font-bold text-brand underline">{order.ref}</button>
                </td>
                <td className={`${td} text-muted`}>{order.outlet}</td>
                <td className={`${td} text-muted`}>{order.brand}</td>
                <td className={`${td} text-right font-medium tabular-nums`}>{order.cases}</td>
                <td className={`${td} text-right font-medium tabular-nums`}>{order.kg.toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 1 })}</td>
                <td className={`${td} text-right font-medium tabular-nums`}>{order.volumeM3.toFixed(2)}</td>
                <td className={`${td} tabular-nums text-muted`}>{order.window}</td>
                <td className={td}><OrderFlags flags={order.flags} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export const OrderQueue: React.FC<OrderQueueProps> = ({
  orders,
  unconfirmedStores,
  runDateLabel,
  close,
  onCloseOrders,
  onAddOrder,
  onEnterByPhone,
  onOpenOrder,
  onCreatePlan,
}) => {
  const [view, setView] = useState<QueueView>(unconfirmedStores.length > 0 ? 'not-confirmed' : 'all');
  const [categoryId, setCategoryId] = useState('all');
  const categories = buildCategories(orders);
  const activeCategory = categories.find((category) => category.id === categoryId) ?? categories[0];
  const visibleOrders = orders.filter(activeCategory.matches);

  return (
    <div className="w-full space-y-6">
      {view === 'not-confirmed' ? <UnconfirmedBanner count={unconfirmedStores.length} runDateLabel={runDateLabel} /> : <StatsRow orders={orders} />}

      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap items-center gap-2">
          <button type="button" aria-pressed={view === 'all' && activeCategory.id === 'all'} onClick={() => { setView('all'); setCategoryId('all'); }} className={`${chip} ${view === 'all' && activeCategory.id === 'all' ? chipActive : chipIdle}`}>
            All {orders.length}
          </button>
          <button type="button" aria-pressed={view === 'not-confirmed'} onClick={() => setView('not-confirmed')} className={`${chip} ${view === 'not-confirmed' ? 'border-brand bg-brand text-on-brand font-semibold' : chipIdle}`}>
            Not confirmed {unconfirmedStores.length}
          </button>
          {view === 'all' && categories.filter((category) => category.id !== 'all').map((category) => (
            <button key={category.id} type="button" aria-pressed={activeCategory.id === category.id} onClick={() => setCategoryId(category.id)} className={`${chip} ${activeCategory.id === category.id ? chipActive : chipIdle}`}>
              {category.label} {orders.filter(category.matches).length}
            </button>
          ))}
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <Button tone="secondary" size="s" onClick={onAddOrder}>+ Add order for outlet</Button>
          <Button tone="secondary" size="s" disabled={close.closed || close.isClosing} onClick={onCloseOrders}>
            {close.isClosing ? 'Closing…' : close.closed ? '✓ Orders closed' : 'Close orders'}
          </Button>
          {view === 'all' && <Button size="s" onClick={onCreatePlan}>Create plan</Button>}
        </div>
      </div>

      <p className="text-xs text-muted">{close.statusText}</p>
      {close.feedback && (
        <p role={close.feedback.tone === 'danger' ? 'alert' : 'status'} className={`rounded-lg p-3 text-sm font-semibold ${close.feedback.tone === 'danger' ? 'bg-status-failed-soft text-status-failed' : 'bg-status-delivered-soft text-status-delivered'}`}>
          {close.feedback.tone === 'danger' ? '✕ ' : '✓ '}{close.feedback.text}
        </p>
      )}

      {view === 'not-confirmed'
        ? <UnconfirmedTable stores={unconfirmedStores} onEnterByPhone={onEnterByPhone} />
        : <OrdersTable orders={visibleOrders} onOpenOrder={onOpenOrder} />}

      <p className="text-xs text-muted">
        {view === 'not-confirmed'
          ? 'Late adds are still allowed after the cut-off until the plan is published — they are tagged "Entered by dispatcher" and the store is asked to check them.'
          : `Showing ${visibleOrders.length} of ${orders.length} · sorted by order reference`}
      </p>
    </div>
  );
};

export default OrderQueue;
