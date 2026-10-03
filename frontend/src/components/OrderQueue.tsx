import React, { useState } from 'react';

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

export const MOCK_ORDERS: OrderItem[] = [
  {
    id: 'S1-001',
    outlet: 'OUT001 · Colombo',
    brand: 'Fresh',
    cases: 80,
    kg: 448.6,
    volumeM3: 2.45,
    window: '5:00–7:30',
    flags: { chilled: true, vanOnly: true },
  },
  {
    id: 'S1-003',
    outlet: 'OUT002 · Colombo',
    brand: 'Fresh',
    cases: 38,
    kg: 329.0,
    volumeM3: 1.84,
    window: '5:30–8:00',
    flags: { chilled: true, vanOnly: true },
  },
  {
    id: 'S1-005',
    outlet: 'OUT003 · Colombo',
    brand: 'Fresh',
    cases: 42,
    kg: 318.1,
    volumeM3: 1.73,
    window: '5:00–7:30',
    flags: { chilled: true, vanOnly: true },
  },
  {
    id: 'S1-014',
    outlet: 'OUT008 · Colombo',
    brand: 'Fresh',
    cases: 102,
    kg: 713.9,
    volumeM3: 4.00,
    window: '5:00–7:30',
    flags: { chilled: true },
  },
  {
    id: 'S1-058',
    outlet: 'OUT054 · Galle',
    brand: 'Fresh',
    cases: 547,
    kg: 2741.8,
    volumeM3: 16.52,
    window: '5:00–7:30',
    flags: { chilled: true },
  },
  {
    id: 'S1-064',
    outlet: 'OUT060 · Matara',
    brand: 'Fresh',
    cases: 186,
    kg: 1277.8,
    volumeM3: 6.78,
    window: '3:00–8:00',
    flags: { chilled: true },
  },
  {
    id: 'S1-078',
    outlet: 'OUT070 · Kurunegala',
    brand: 'Style',
    cases: 42,
    kg: 2561.6,
    volumeM3: 40.66,
    window: '9:00 AM – 5:00 PM',
    flags: { largerThanVehicle: true, waitedDays: 2 },
  },
  {
    id: 'S1-083',
    outlet: 'OUT074 · Puttalam',
    brand: 'Fresh',
    cases: 205,
    kg: 1588.8,
    volumeM3: 8.66,
    window: '5:30–8:00',
    flags: { chilled: true, waitedDays: 5, skippedYesterday: true },
  },
];

type FilterChip =
  | 'All 85'
  | 'Fresh 75'
  | 'Style 5'
  | 'Tech 5'
  | 'Chilled 26'
  | 'Van only 6'
  | 'Skipped before 10';

export interface OrderQueueProps {
  orders?: OrderItem[];
  onAddOrder?: () => void;
  onCreatePlan?: () => void;
}

export const OrderQueue: React.FC<OrderQueueProps> = ({
  orders = MOCK_ORDERS,
  onAddOrder,
  onCreatePlan,
}) => {
  const [activeChip, setActiveChip] = useState<FilterChip>('All 85');

  const filterChips: FilterChip[] = [
    'All 85',
    'Fresh 75',
    'Style 5',
    'Tech 5',
    'Chilled 26',
    'Van only 6',
    'Skipped before 10',
  ];

  return (
    <div className="w-full space-y-6 font-sans">
      {/* 1. Top Section: 4 Summary Stats Cards */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Card 1: CONFIRMED ORDERS */}
        <div className="flex flex-col justify-between rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
          <div>
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              CONFIRMED ORDERS
            </span>
            <div className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">
              85
            </div>
          </div>
          <p className="mt-3 text-xs text-slate-500">Fresh 75 · Style 5 · Tech 5</p>
        </div>

        {/* Card 2: CHILLED (Amber / Brown styling) */}
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

        {/* Card 3: VAN-ONLY OUTLETS */}
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

        {/* Card 4: CARRIED OVER (Pink / Red styling) */}
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
      </section>

      {/* 2. Middle Section: Filter Chips & Action Buttons */}
      <section className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        {/* Filter Chips */}
        <div className="flex flex-wrap items-center gap-2">
          {filterChips.map((chip) => {
            const isActive = activeChip === chip;
            return (
              <button
                key={chip}
                type="button"
                onClick={() => setActiveChip(chip)}
                className={`min-h-[40px] rounded-full px-3.5 py-1.5 text-xs font-medium transition-all ${
                  isActive
                    ? 'border border-blue-600 bg-blue-50/80 font-semibold text-blue-700 shadow-2xs'
                    : 'border border-slate-300 bg-white text-slate-700 hover:border-slate-400 hover:bg-slate-50'
                }`}
              >
                {chip}
              </button>
            );
          })}
        </div>

        {/* Action Buttons */}
        <div className="flex flex-wrap items-center gap-3">
          {/* "+ Add order for outlet" Button */}
          <button
            type="button"
            onClick={onAddOrder}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-lg border border-blue-600 bg-white px-4 py-2 text-sm font-semibold text-blue-700 shadow-2xs transition hover:bg-blue-50"
          >
            <svg
              className="h-4 w-4 stroke-[2.5]"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
            >
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>Add order for outlet</span>
          </button>

          {/* "Create plan" Button with Wand Icon */}
          <button
            type="button"
            onClick={onCreatePlan}
            className="inline-flex min-h-[40px] items-center gap-2 rounded-lg bg-[#0e2a47] px-4 py-2 text-sm font-semibold text-white shadow-xs transition hover:bg-[#14365b]"
          >
            <svg
              className="h-4 w-4 stroke-[2]"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
            >
              <path d="m19 11-4-4" />
              <path d="m5 21 10-10" />
              <path d="M19 5l-2-2" />
              <path d="M2 2l2 2" />
              <path d="m14 2 1 3 3 1-3 1-1 3-1-3-3-1 3-1 1-3z" />
            </svg>
            <span>Create plan</span>
          </button>
        </div>
      </section>

      {/* 3. Detailed Data Table */}
      <section className="overflow-hidden rounded-xl border border-slate-200/90 bg-white shadow-2xs">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left text-sm">
            {/* Table Headers */}
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

            {/* Table Body */}
            <tbody className="divide-y divide-slate-100 text-slate-800">
              {orders.map((order) => (
                <tr
                  key={order.id}
                  className="transition-colors hover:bg-slate-50/60"
                >
                  {/* Order Column */}
                  <td className="px-5 py-4 font-bold text-slate-900 whitespace-nowrap">
                    {order.id}
                  </td>

                  {/* Outlet Column */}
                  <td className="px-5 py-4 text-slate-600 whitespace-nowrap">
                    {order.outlet}
                  </td>

                  {/* Brand Column */}
                  <td className="px-5 py-4 text-slate-600 whitespace-nowrap">
                    {order.brand}
                  </td>

                  {/* Cases Column */}
                  <td className="px-5 py-4 text-right font-medium text-slate-700 tabular-nums whitespace-nowrap">
                    {order.cases}
                  </td>

                  {/* kg Column */}
                  <td className="px-5 py-4 text-right font-medium text-slate-700 tabular-nums whitespace-nowrap">
                    {order.kg.toLocaleString(undefined, {
                      minimumFractionDigits: 1,
                      maximumFractionDigits: 1,
                    })}
                  </td>

                  {/* m³ Column */}
                  <td className="px-5 py-4 text-right font-medium text-slate-700 tabular-nums whitespace-nowrap">
                    {order.volumeM3.toFixed(2)}
                  </td>

                  {/* Window Column */}
                  <td className="px-5 py-4 text-slate-600 tabular-nums whitespace-nowrap">
                    {order.window}
                  </td>

                  {/* Flags Column */}
                  <td className="px-5 py-4 whitespace-nowrap">
                    <div className="flex flex-wrap items-center gap-1.5">
                      {/* CHILLED Flag (Sky Blue Outline) */}
                      {order.flags.chilled && (
                        <span className="inline-flex items-center rounded border border-sky-400 bg-sky-50/60 px-2 py-0.5 text-[10px] font-bold tracking-wide text-sky-700 uppercase">
                          CHILLED
                        </span>
                      )}

                      {/* VAN ONLY Flag (Slate Outline) */}
                      {order.flags.vanOnly && (
                        <span className="inline-flex items-center rounded border border-slate-300 bg-slate-50 px-2 py-0.5 text-[10px] font-bold tracking-wide text-slate-600 uppercase">
                          VAN ONLY
                        </span>
                      )}

                      {/* "Larger than any vehicle" Flag (Red Alert Pill) */}
                      {order.flags.largerThanVehicle && (
                        <span className="inline-flex items-center gap-1 rounded-full border border-red-200 bg-red-50/90 px-2.5 py-0.5 text-xs font-medium text-red-700">
                          <svg
                            className="h-3.5 w-3.5 stroke-[2]"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                          >
                            <circle cx="12" cy="12" r="10" />
                            <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
                          </svg>
                          <span>Larger than any vehicle</span>
                        </span>
                      )}

                      {/* "Waited X days" Flag (Pink Calendar Pill) */}
                      {order.flags.waitedDays !== undefined && (
                        <span className="inline-flex items-center gap-1 rounded-full border border-pink-200 bg-pink-50/80 px-2.5 py-0.5 text-xs font-medium text-pink-700">
                          <svg
                            className="h-3.5 w-3.5 text-pink-600 stroke-[2]"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                          >
                            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                            <line x1="16" y1="2" x2="16" y2="6" />
                            <line x1="8" y1="2" x2="8" y2="6" />
                            <line x1="3" y1="10" x2="21" y2="10" />
                          </svg>
                          <span>Waited {order.flags.waitedDays} days</span>
                        </span>
                      )}

                      {/* "SKIPPED YESTERDAY" Flag (Rose Outline Uppercase) */}
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
      </section>

      {/* Bottom Priority Sorting Caption */}
      <footer className="text-xs text-slate-500">
        Showing 8 of 85 · sorted by priority (days waited, chilled, window)
      </footer>
    </div>
  );
};

export default OrderQueue;
