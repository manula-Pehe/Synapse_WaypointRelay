import React from 'react';

export interface TimelineEvent {
  id: string;
  title: string;
  timestamp: string;
  dotColor: 'green' | 'blue' | 'brown';
}

export interface OrderHistoryDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  orderId?: string;
  outletId?: string;
  casesSummary?: string;
  onRecordOutcome?: () => void;
  onPrintLoadingList?: () => void;
}

const DEFAULT_TIMELINE_EVENTS: TimelineEvent[] = [
  {
    id: 'evt-1',
    title: 'Prepared from order history',
    timestamp: 'Wed 6:00 AM',
    dotColor: 'green',
  },
  {
    id: 'evt-2',
    title: 'Confirmed by Dilani J. (store)',
    timestamp: 'Wed 2:10 PM',
    dotColor: 'green',
  },
  {
    id: 'evt-3',
    title: 'Planned · plan v1 · VEH036 trip 2 stop 1',
    timestamp: 'Wed 7:10 PM · published by Ruwan',
    dotColor: 'blue',
  },
  {
    id: 'evt-4',
    title: 'Loaded by Kasun (dock PIN)',
    timestamp: 'Thu 6:08 AM · 70 cases',
    dotColor: 'blue',
  },
  {
    id: 'evt-5',
    title: 'Arrival window updated 7:05–7:35',
    timestamp: 'Thu 6:41 AM · risk watch',
    dotColor: 'brown',
  },
  {
    id: 'evt-6',
    title: 'Delivered 78 of 80 · 2 damaged',
    timestamp: 'Thu 7:14 AM · photo + signature · VEH036',
    dotColor: 'brown',
  },
  {
    id: 'evt-7',
    title: 'Remainder order S1-001-R · 2 cases',
    timestamp: 'Fri 2 Oct run',
    dotColor: 'blue',
  },
  {
    id: 'evt-8',
    title: 'Received — confirmed by store',
    timestamp: 'Thu 7:20 AM · Dilani J.',
    dotColor: 'green',
  },
];

export const OrderHistoryDrawer: React.FC<OrderHistoryDrawerProps> = ({
  isOpen,
  onClose,
  orderId = 'S1-001',
  outletId = 'OUT001',
  casesSummary = 'Chilled 70 cases',
  onRecordOutcome,
  onPrintLoadingList,
}) => {
  if (!isOpen) return null;

  const dotColorClasses = {
    green: 'bg-[#0f766e]', // Dark emerald / teal dot
    blue: 'bg-[#2563eb]', // Vibrant blue dot
    brown: 'bg-[#92400e]', // Amber-brown dot
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end font-sans">
      {/* 1. Backdrop Overlay */}
      <div
        className="fixed inset-0 bg-slate-900/40 backdrop-blur-2xs transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* 2. Slide-out Drawer Panel */}
      <div className="relative z-10 flex h-full w-full max-w-md flex-col bg-white shadow-2xl sm:max-w-lg">
        {/* Header */}
        <div className="border-b border-slate-100 p-6 pb-4">
          <div className="flex items-start justify-between">
            <h2 className="text-xl font-bold tracking-tight text-slate-900">
              {orderId} · {outletId} · {casesSummary}
            </h2>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close drawer"
              className="flex min-h-[40px] min-w-[40px] items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
            >
              <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Badges below title */}
          <div className="mt-2.5 flex flex-wrap items-center gap-2">
            {/* Warning / Partial Badge */}
            <span className="inline-flex items-center gap-1.5 rounded-md border border-amber-200 bg-amber-50/80 px-2 py-0.5 text-xs font-medium text-amber-800">
              <svg className="h-3.5 w-3.5 text-amber-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
              </svg>
              <span>Partial · 78 of 80</span>
            </span>

            {/* Remainder Badge */}
            <span className="inline-flex items-center gap-1.5 rounded-md border border-sky-200 bg-sky-50 px-2 py-0.5 text-xs font-medium text-sky-800">
              <svg className="h-3.5 w-3.5 text-sky-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M5 18H3c-.6 0-1-.4-1-1V9c0-.6.4-1 1-1h10c.6 0 1 .4 1 1v8c0 .6-.4 1-1 1h-2" />
                <path d="M14 9h4l4 4v4c0 .6-.4 1-1 1h-2" />
                <circle cx="7" cy="18" r="2" />
                <circle cx="17" cy="18" r="2" />
              </svg>
              <span>Remainder Fri</span>
            </span>
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* 3. History Section Card */}
          <div className="rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
            <h3 className="text-sm font-bold text-slate-900 mb-5">History</h3>

            {/* Vertical Timeline */}
            <div className="relative">
              {DEFAULT_TIMELINE_EVENTS.map((event, index) => {
                const isLast = index === DEFAULT_TIMELINE_EVENTS.length - 1;
                return (
                  <div key={event.id} className="relative flex items-start gap-4 pb-5 last:pb-0">
                    {/* Vertical connecting line */}
                    {!isLast && (
                      <span
                        className="absolute left-[5px] top-[14px] -bottom-[2px] w-[1.5px] bg-slate-200"
                        aria-hidden="true"
                      />
                    )}

                    {/* Timeline Dot */}
                    <div className="relative mt-1 flex h-3 w-3 flex-shrink-0 items-center justify-center">
                      <span className={`h-2.5 w-2.5 rounded-full ${dotColorClasses[event.dotColor]}`} />
                    </div>

                    {/* Event Content */}
                    <div className="flex flex-col">
                      <span className="text-xs font-semibold text-slate-900">
                        {event.title}
                      </span>
                      <span className="mt-0.5 text-[11px] text-slate-500">
                        {event.timestamp}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* 4. Actions Section Card */}
          <div className="rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs space-y-3.5">
            <h3 className="text-sm font-bold text-slate-900">Actions</h3>

            {/* Button 1: Record outcome manually */}
            <div>
              <button
                type="button"
                onClick={onRecordOutcome}
                className="inline-flex min-h-[42px] w-full items-center justify-center gap-2 rounded-lg border border-blue-600 bg-white px-4 py-2 text-sm font-semibold text-blue-700 shadow-2xs hover:bg-blue-50 transition"
              >
                <svg className="h-4 w-4 stroke-[2]" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" />
                  <path d="m15 5 4 4" />
                </svg>
                <span>Record outcome manually</span>
              </button>
              <p className="mt-1.5 text-center text-xs text-slate-400">
                For when a driver's phone is dead or lost. Tagged “entered by dispatcher”.
              </p>
            </div>

            {/* Button 2: Print loading list */}
            <div>
              <button
                type="button"
                onClick={onPrintLoadingList}
                className="inline-flex min-h-[42px] w-full items-center justify-center gap-2 rounded-lg border border-blue-600 bg-white px-4 py-2 text-sm font-semibold text-blue-700 shadow-2xs hover:bg-blue-50 transition"
              >
                <svg className="h-4 w-4 stroke-[2]" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <polyline points="6 9 6 2 18 2 18 9" />
                  <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
                  <rect x="6" y="14" width="12" height="8" />
                </svg>
                <span>Print loading list (VEH036 trip 2)</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderHistoryDrawer;
