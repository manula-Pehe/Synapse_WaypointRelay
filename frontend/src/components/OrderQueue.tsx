import React, { useState } from 'react';
import AddOrderDrawer from './AddOrderDrawer';
import OrderHistoryDrawer from './OrderHistoryDrawer';

// --- Types for All Orders View ---
export interface OrderItem {
  id: string;
  outlet: string;
  brand: 'Fresh' | 'Style' | 'Tech';
  cases: number;
  kg: number;
  volumeM3: number;
  window: string;
  flags: {
    chilled?: boolean;
    vanOnly?: boolean;
    largerThanVehicle?: boolean;
    waitedDays?: number;
    skippedYesterday?: boolean;
  };
}

// --- Types for Unconfirmed Stores View ---
export interface UnconfirmedStore {
  id: string;
  outlet: string;
  preparedOrder: string;
  lastSeen: string;
  contact: string;
}

// --- Mock Data ---
const MOCK_ORDERS: OrderItem[] = [
  {
    id: 'S1-001',
    outlet: 'OUT001 · Colombo',
    brand: 'Fresh',
    cases: 70,
    kg: 500.0,
    volumeM3: 3.0,
    window: '5:00–7:00',
    flags: { chilled: true, vanOnly: true },
  },
  {
    id: 'S1-003',
    outlet: 'OUT002 · Colombo',
    brand: 'Fresh',
    cases: 30,
    kg: 250.0,
    volumeM3: 1.5,
    window: '5:15–7:45',
    flags: { chilled: true, vanOnly: true },
  },
  {
    id: 'S1-005',
    outlet: 'OUT003 · Colombo',
    brand: 'Fresh',
    cases: 50,
    kg: 400.0,
    volumeM3: 2.0,
    window: '5:15–7:45',
    flags: { chilled: true, vanOnly: true },
  },
  {
    id: 'S1-014',
    outlet: 'OUT008 · Colombo',
    brand: 'Fresh',
    cases: 100,
    kg: 800.0,
    volumeM3: 5.0,
    window: '5:15–7:45',
    flags: { chilled: true },
  },
  {
    id: 'S1-058',
    outlet: 'OUT054 · Galle',
    brand: 'Fresh',
    cases: 500,
    kg: 3000.0,
    volumeM3: 18.0,
    window: '5:15–7:45',
    flags: { chilled: true },
  },
  {
    id: 'S1-064',
    outlet: 'OUT060 · Matara',
    brand: 'Fresh',
    cases: 200,
    kg: 1500.0,
    volumeM3: 7.5,
    window: '3:30–8:00',
    flags: { chilled: true },
  },
  {
    id: 'S1-078',
    outlet: 'OUT070 · Kurunegala',
    brand: 'Style',
    cases: 40,
    kg: 2500.0,
    volumeM3: 40.0,
    window: '10:00 AM – 5:00 PM',
    flags: { largerThanVehicle: true, waitedDays: 2 },
  },
  {
    id: 'S1-083',
    outlet: 'OUT074 · Puttalam',
    brand: 'Fresh',
    cases: 200,
    kg: 1600.0,
    volumeM3: 9.0,
    window: '5:15–7:45',
    flags: { chilled: true, waitedDays: 5, skippedYesterday: true },
  },
];

const MOCK_UNCONFIRMED_STORES: UnconfirmedStore[] = [
  {
    id: 'OUT012',
    outlet: 'OUT012 · Colombo',
    preparedOrder: 'Chilled · 64 cases',
    lastSeen: 'Today 9:10 AM',
    contact: 'Store manager · 011 234 5566',
  },
  {
    id: 'OUT045',
    outlet: 'OUT045 · Kalutara',
    preparedOrder: 'Chilled · 40 cases',
    lastSeen: 'Yesterday',
    contact: 'Store manager · 034 222 1180',
  },
  {
    id: 'OUT088',
    outlet: 'OUT088 · Kandy',
    preparedOrder: 'Chilled · 52 cases',
    lastSeen: 'Today 11:45 AM',
    contact: 'Store manager · 033 223 4410',
  },
  {
    id: 'OUT104',
    outlet: 'OUT104 · Nuwara Eliya',
    preparedOrder: 'Chilled · 30 cases',
    lastSeen: 'Mon 28 Sep',
    contact: 'Store manager · 052 223 7700',
  },
];

export interface OrderQueueProps {
  orders?: OrderItem[];
  unconfirmedStores?: UnconfirmedStore[];
  onAddOrder?: () => void;
  onCreatePlan?: () => void;
  onSendReminderAgain?: () => void;
  onEnterByPhone?: (storeId: string) => void;
}

export const OrderQueue: React.FC<OrderQueueProps> = ({
  orders = MOCK_ORDERS,
  unconfirmedStores = MOCK_UNCONFIRMED_STORES,
  onAddOrder,
  onCreatePlan,
  onSendReminderAgain,
  onEnterByPhone,
}) => {
  // State for view navigation
  const [activeFilter, setActiveFilter] = useState<'all' | 'not-confirmed'>('not-confirmed');

  // Sub-filter category when in 'all' view
  const [allViewCategory, setAllViewCategory] = useState<string>('All 80');

  // Drawer open/close state for Add Order
  const [isAddOrderOpen, setIsAddOrderOpen] = useState(false);

  // Drawer state for Order History
  const [selectedOrderForHistory, setSelectedOrderForHistory] = useState<OrderItem | null>(null);

  const handleOpenAddOrder = () => {
    setIsAddOrderOpen(true);
    onAddOrder?.();
  };

  const handleEnterByPhone = (storeId: string) => {
    setIsAddOrderOpen(true);
    onEnterByPhone?.(storeId);
  };

  return (
    <div className="w-full space-y-6 font-sans">
      {/* 3. Conditional Rendering: HIDE stats cards and SHOW amber banner when activeFilter === 'not-confirmed' */}
      {activeFilter === 'not-confirmed' ? (
        /* Amber Warning Banner */
        <div className="flex flex-col justify-between gap-4 rounded-xl border border-amber-300 bg-amber-50/80 p-5 shadow-2xs md:flex-row md:items-center">
          <div className="flex items-start gap-3.5">
            <div className="mt-0.5 flex-shrink-0 text-amber-700">
              <svg
                className="h-5 w-5"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"
                />
              </svg>
            </div>
            <div>
              <h2 className="text-base font-bold text-amber-900">
                4 stores haven't confirmed their chilled order for Thursday
              </h2>
              <p className="mt-1 text-xs text-amber-800/90 sm:text-sm">
                They got the 3:00 PM reminder. Unconfirmed chilled orders are not planned after 4:00 PM — call them, or enter the order for them if they tell you by phone.
              </p>
            </div>
          </div>

          {/* "Send reminder again" button */}
          <button
            type="button"
            onClick={onSendReminderAgain}
            className="inline-flex min-h-[40px] flex-shrink-0 items-center gap-2 rounded-lg border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 shadow-2xs transition hover:bg-slate-50"
          >
            <svg
              className="h-4 w-4 text-slate-600"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"
              />
            </svg>
            <span>Send reminder again</span>
          </button>
        </div>
      ) : (
        /* Top 4 Summary Stats Cards for 'all' orders */
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="flex flex-col justify-between rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
            <div>
              <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
                CONFIRMED ORDERS
              </span>
              <div className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">
                80
              </div>
            </div>
            <p className="mt-3 text-xs text-slate-500">Fresh 60 · Style 12 · Tech 8</p>
          </div>

          <div className="flex flex-col justify-between rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
            <div>
              <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
                CHILLED
              </span>
              <div className="mt-2 text-3xl font-extrabold tracking-tight text-amber-800">
                26 · 182 <span className="text-2xl font-bold">m³</span>
              </div>
            </div>
            <p className="mt-3 text-xs text-slate-500">
              3× a normal day — only 4 fridge vehicles
            </p>
          </div>

          <div className="flex flex-col justify-between rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
            <div>
              <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
                VAN-ONLY OUTLETS
              </span>
              <div className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">
                6 <span className="text-2xl font-bold">orders</span>
              </div>
            </div>
            <p className="mt-3 text-xs text-slate-500">3 chilled · only 1 fridge van</p>
          </div>

          <div className="flex flex-col justify-between rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
            <div>
              <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
                CARRIED OVER
              </span>
              <div className="mt-2 text-3xl font-extrabold tracking-tight text-rose-700">
                10
              </div>
            </div>
            <p className="mt-3 text-xs text-slate-500">Skipped before · priority raised</p>
          </div>
        </div>
      )}

      {/* 2. Filter Chips Row */}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap items-center gap-2">
          {/* 'All 80' Chip */}
          <button
            type="button"
            onClick={() => {
              setActiveFilter('all');
              setAllViewCategory('All 80');
            }}
            className={`min-h-[40px] rounded-full px-3.5 py-1.5 text-xs font-medium transition-all ${
              activeFilter === 'all' && allViewCategory === 'All 80'
                ? 'border border-blue-600 bg-blue-50/80 font-semibold text-blue-700 shadow-2xs'
                : 'border border-slate-300 bg-white text-slate-700 hover:border-slate-400 hover:bg-slate-50'
            }`}
          >
            All 80
          </button>

          {/* 'Not confirmed 4' Chip */}
          <button
            type="button"
            onClick={() => setActiveFilter('not-confirmed')}
            className={`min-h-[40px] rounded-full px-3.5 py-1.5 text-xs transition-all ${
              activeFilter === 'not-confirmed'
                ? 'bg-[#0e2a47] font-semibold text-white shadow-xs'
                : 'border border-slate-300 bg-white font-medium text-slate-700 hover:border-slate-400 hover:bg-slate-50'
            }`}
          >
            Not confirmed 4
          </button>

          {/* Category Chips */}
          {activeFilter === 'not-confirmed' ? (
            <>
              <button
                type="button"
                className="min-h-[40px] rounded-full border border-slate-300 bg-white px-3.5 py-1.5 text-xs font-medium text-slate-700 hover:border-slate-400 hover:bg-slate-50"
              >
                Chilled 20
              </button>
              <button
                type="button"
                className="min-h-[40px] rounded-full border border-slate-300 bg-white px-3.5 py-1.5 text-xs font-medium text-slate-700 hover:border-slate-400 hover:bg-slate-50"
              >
                Van only 12
              </button>
            </>
          ) : (
            <>
              {(['Fresh 60', 'Style 12', 'Tech 8', 'Chilled 20', 'Van only 12', 'Skipped before 7'] as const).map(
                (cat) => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => setAllViewCategory(cat)}
                    className={`min-h-[40px] rounded-full px-3.5 py-1.5 text-xs font-medium transition-all ${
                      allViewCategory === cat
                        ? 'border border-blue-600 bg-blue-50/80 font-semibold text-blue-700 shadow-2xs'
                        : 'border border-slate-300 bg-white text-slate-700 hover:border-slate-400 hover:bg-slate-50'
                    }`}
                  >
                    {cat}
                  </button>
                )
              )}
            </>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={handleOpenAddOrder}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-lg border border-blue-600 bg-white px-4 py-2 text-sm font-semibold text-blue-700 shadow-2xs transition hover:bg-blue-50"
          >
            <svg className="h-4 w-4 stroke-[2.5]" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>Add order for outlet</span>
          </button>

          {activeFilter === 'all' && (
            <button
              type="button"
              onClick={onCreatePlan}
              className="inline-flex min-h-[40px] items-center gap-2 rounded-lg bg-[#0e2a47] px-4 py-2 text-sm font-semibold text-white shadow-xs transition hover:bg-[#14365b]"
            >
              <svg className="h-4 w-4 stroke-[2]" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="m19 11-4-4" />
                <path d="m5 21 10-10" />
                <path d="M19 5l-2-2" />
                <path d="M2 2l2 2" />
                <path d="m14 2 1 3 3 1-3 1-1 3-1-3-3-1 3-1 1-3z" />
              </svg>
              <span>Create plan</span>
            </button>
          )}
        </div>
      </div>

      {/* 4. Table Conditional Rendering */}
      {activeFilter === 'not-confirmed' ? (
        /* New "Unconfirmed Stores" Table */
        <div className="overflow-hidden rounded-xl border border-slate-200/90 bg-white shadow-2xs">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/80 text-xs font-semibold tracking-wider text-slate-500 uppercase">
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">OUTLET</th>
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">PREPARED ORDER</th>
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">LAST SEEN IN APP</th>
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">CONTACT</th>
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">ACTIONS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-800">
                {unconfirmedStores.map((store) => (
                  <tr
                    key={store.id}
                    className="transition-colors hover:bg-slate-50/60"
                  >
                    <td className="px-5 py-4 font-bold text-slate-900 whitespace-nowrap">
                      {store.outlet}
                    </td>
                    <td className="px-5 py-4 text-slate-600 whitespace-nowrap">
                      {store.preparedOrder}
                    </td>
                    <td className="px-5 py-4 text-slate-600 whitespace-nowrap">
                      {store.lastSeen}
                    </td>
                    <td className="px-5 py-4 text-slate-600 whitespace-nowrap">
                      {store.contact}
                    </td>
                    <td className="px-5 py-4 whitespace-nowrap">
                      <button
                        type="button"
                        onClick={() => handleEnterByPhone(store.id)}
                        className="inline-flex min-h-[36px] items-center gap-2 rounded-lg border border-blue-600 bg-white px-3.5 py-1.5 text-xs font-semibold text-blue-700 shadow-2xs transition hover:bg-blue-50"
                      >
                        <svg
                          className="h-3.5 w-3.5"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M2.25 6.75c0 8.284 6.716 15 15 15h2.25a2.25 2.25 0 002.25-2.25v-1.372c0-.516-.351-.966-.852-1.091l-4.423-1.106c-.44-.11-.902.055-1.173.417l-.97 1.293c-.282.376-.769.542-1.21.38a12.035 12.035 0 01-7.143-7.143c-.162-.441.004-.928.38-1.21l1.293-.97c.363-.271.527-.734.417-1.173L6.963 3.102a1.125 1.125 0 00-1.091-.852H4.5A2.25 2.25 0 002.25 4.5v2.25z"
                          />
                        </svg>
                        <span>Enter by phone</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        /* Standard All Orders Table */
        <div className="overflow-hidden rounded-xl border border-slate-200/90 bg-white shadow-2xs">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/80 text-xs font-semibold text-slate-500">
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Order</th>
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Outlet</th>
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Brand</th>
                  <th scope="col" className="px-5 py-3.5 text-right whitespace-nowrap">Cases</th>
                  <th scope="col" className="px-5 py-3.5 text-right whitespace-nowrap">kg</th>
                  <th scope="col" className="px-5 py-3.5 text-right whitespace-nowrap">m³</th>
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Window</th>
                  <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Flags</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-800">
                {orders.map((order) => (
                  <tr
                    key={order.id}
                    onClick={() => setSelectedOrderForHistory(order)}
                    className="cursor-pointer transition-colors hover:bg-slate-50/80"
                  >
                    <td className="px-5 py-4 font-bold text-slate-900 whitespace-nowrap">
                      {order.id}
                    </td>
                    <td className="px-5 py-4 text-slate-600 whitespace-nowrap">
                      {order.outlet}
                    </td>
                    <td className="px-5 py-4 text-slate-600 whitespace-nowrap">
                      {order.brand}
                    </td>
                    <td className="px-5 py-4 text-right font-medium text-slate-700 tabular-nums whitespace-nowrap">
                      {order.cases}
                    </td>
                    <td className="px-5 py-4 text-right font-medium text-slate-700 tabular-nums whitespace-nowrap">
                      {order.kg.toLocaleString(undefined, {
                        minimumFractionDigits: 1,
                        maximumFractionDigits: 1,
                      })}
                    </td>
                    <td className="px-5 py-4 text-right font-medium text-slate-700 tabular-nums whitespace-nowrap">
                      {order.volumeM3.toFixed(2)}
                    </td>
                    <td className="px-5 py-4 text-slate-600 tabular-nums whitespace-nowrap">
                      {order.window}
                    </td>
                    <td className="px-5 py-4 whitespace-nowrap">
                      <div className="flex flex-wrap items-center gap-1.5">
                        {order.flags.chilled && (
                          <span className="inline-flex items-center rounded border border-sky-400 bg-sky-50/60 px-2 py-0.5 text-[10px] font-bold tracking-wide text-sky-700 uppercase">
                            CHILLED
                          </span>
                        )}
                        {order.flags.vanOnly && (
                          <span className="inline-flex items-center rounded border border-slate-300 bg-slate-50 px-2 py-0.5 text-[10px] font-bold tracking-wide text-slate-600 uppercase">
                            VAN ONLY
                          </span>
                        )}
                        {order.flags.largerThanVehicle && (
                          <span className="inline-flex items-center gap-1 rounded-full border border-red-200 bg-red-50/90 px-2.5 py-0.5 text-xs font-medium text-red-700">
                            <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                              <circle cx="12" cy="12" r="10" />
                              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
                            </svg>
                            <span>Larger than any vehicle</span>
                          </span>
                        )}
                        {order.flags.waitedDays !== undefined && (
                          <span className="inline-flex items-center gap-1 rounded-full border border-pink-200 bg-pink-50/80 px-2.5 py-0.5 text-xs font-medium text-pink-700">
                            <svg className="h-3.5 w-3.5 text-pink-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                              <line x1="16" y1="2" x2="16" y2="6" />
                              <line x1="8" y1="2" x2="8" y2="6" />
                              <line x1="3" y1="10" x2="21" y2="10" />
                            </svg>
                            <span>Waited {order.flags.waitedDays} days</span>
                          </span>
                        )}
                        {order.flags.skippedYesterday && (
                          <span className="inline-flex items-center rounded border border-pink-400 bg-pink-50/70 px-2 py-0.5 text-[10px] font-bold tracking-wide text-pink-700 uppercase">
                            SKIPPED YESTERDAY
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 5. Bottom Info / Status Text */}
      {activeFilter === 'not-confirmed' ? (
        <div className="flex items-center gap-2 text-xs text-slate-500">
          <svg
            className="h-4 w-4 flex-shrink-0 text-slate-400"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
          <span>
            Late adds are still allowed after 4:00 PM until the plan is published — they are tagged "Entered by dispatcher" and the store is asked to check them.
          </span>
        </div>
      ) : (
        <div className="text-xs text-slate-500">
          Showing 8 of 80 · sorted by priority (days waited, chilled, window)
        </div>
      )}

      {/* Add Order Slide-out Drawer */}
      <AddOrderDrawer
        isOpen={isAddOrderOpen}
        onClose={() => setIsAddOrderOpen(false)}
      />

      {/* Order History Slide-out Drawer */}
      <OrderHistoryDrawer
        isOpen={!!selectedOrderForHistory}
        onClose={() => setSelectedOrderForHistory(null)}
        orderId={selectedOrderForHistory?.id}
        outletId={selectedOrderForHistory?.outlet.split(' ')[0]}
        casesSummary={`${selectedOrderForHistory?.brand === 'Fresh' ? 'Chilled' : 'Style'} ${selectedOrderForHistory?.cases} cases`}
      />
    </div>
  );
};

export default OrderQueue;
