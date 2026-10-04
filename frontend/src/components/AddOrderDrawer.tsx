import React, { useState } from 'react';

export interface AddOrderDrawerProps {
  isOpen?: boolean;
  onClose: () => void;
  onSubmit?: (orderData: {
    outlet: string;
    line: 'dry' | 'chilled';
    runDate: string;
    cases: number;
    phoneContact: string;
  }) => void;
}

export const AddOrderDrawer: React.FC<AddOrderDrawerProps> = ({
  isOpen = true,
  onClose,
  onSubmit,
}) => {
  const [selectedLine, setSelectedLine] = useState<'dry' | 'chilled'>('chilled');
  const [cases, setCases] = useState<number>(64);
  const outlet = 'OUT012 · Waypoint Fresh · Colombo';
  const runDate = 'Thu 1 Oct';
  const phoneContact = 'Store manager, OUT012 · 3:41 PM';

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit?.({
      outlet,
      line: selectedLine,
      runDate,
      cases,
      phoneContact,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      {/* 1. Backdrop Overlay */}
      <div
        className="fixed inset-0 bg-slate-900/40 backdrop-blur-2xs transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* 2. Slide-out Drawer Panel */}
      <div className="relative z-10 flex h-full w-full max-w-md flex-col bg-white shadow-2xl sm:max-w-lg font-sans">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-slate-100 p-6 pb-4">
          <div>
            <h2 className="text-xl font-bold tracking-tight text-slate-900">
              Add order for an outlet
            </h2>
            <p className="mt-1 text-xs text-slate-500">
              For stores that phone in. The store gets an in-app notice to check it.
            </p>
          </div>
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

        {/* Scrollable Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
          {/* Field: Outlet */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Outlet</label>
            <button
              type="button"
              className="flex min-h-[44px] w-full items-center justify-between rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-left text-sm text-slate-900 shadow-2xs hover:border-slate-400 transition"
            >
              <div className="flex items-center gap-2.5 truncate">
                <svg className="h-4 w-4 flex-shrink-0 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                  <polyline points="9 22 9 12 15 12 15 22" />
                </svg>
                <span className="truncate">{outlet}</span>
              </div>
              <svg className="h-4 w-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>

          {/* Field: Line Segmented Buttons */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Line</label>
            <div className="grid grid-cols-2 gap-3">
              {/* Fresh dry */}
              <button
                type="button"
                onClick={() => setSelectedLine('dry')}
                className={`flex min-h-[44px] items-center justify-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium transition ${
                  selectedLine === 'dry'
                    ? 'border-2 border-blue-600 bg-blue-50/70 font-semibold text-blue-900 shadow-2xs'
                    : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'
                }`}
              >
                <svg className="h-4 w-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
                  <path d="m3.3 7 8.7 5 8.7-5" />
                  <path d="M12 22V12" />
                </svg>
                <span>Fresh dry</span>
              </button>

              {/* Fresh chilled (Selected) */}
              <button
                type="button"
                onClick={() => setSelectedLine('chilled')}
                className={`flex min-h-[44px] items-center justify-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium transition ${
                  selectedLine === 'chilled'
                    ? 'border-2 border-blue-600 bg-blue-50/70 font-semibold text-blue-900 shadow-2xs'
                    : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'
                }`}
              >
                <svg className="h-4 w-4 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M2 12h20M12 2v20M20 16l-4-4 4-4M4 8l4 4-4 4M16 4l-4 4-4-4M8 20l4-4 4 4" />
                </svg>
                <span>Fresh chilled</span>
              </button>
            </div>
            <p className="text-xs text-slate-400">
              Only the lines this outlet's brand sells are shown.
            </p>
          </div>

          {/* Fields: Run & Cases Side-by-Side */}
          <div className="grid grid-cols-2 gap-4">
            {/* Run */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Run</label>
              <button
                type="button"
                className="flex min-h-[44px] w-full items-center justify-between rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-left text-sm text-slate-900 shadow-2xs hover:border-slate-400 transition"
              >
                <span>{runDate}</span>
                <svg className="h-4 w-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                </svg>
              </button>
            </div>

            {/* Cases */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Cases</label>
              <input
                type="number"
                min="1"
                value={cases}
                onChange={(e) => setCases(parseInt(e.target.value) || 0)}
                className="min-h-[44px] w-full rounded-lg border-2 border-blue-600 bg-white px-3.5 py-2 text-sm font-bold text-slate-900 shadow-2xs focus:outline-none focus:ring-2 focus:ring-blue-600/20"
              />
            </div>
          </div>

          {/* Field: Taken by phone from */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Taken by phone from</label>
            <div className="flex min-h-[44px] items-center gap-2.5 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-700 shadow-2xs">
              <svg className="h-4 w-4 flex-shrink-0 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 6.75c0 8.284 6.716 15 15 15h2.25a2.25 2.25 0 002.25-2.25v-1.372c0-.516-.351-.966-.852-1.091l-4.423-1.106c-.44-.11-.902.055-1.173.417l-.97 1.293c-.282.376-.769.542-1.21.38a12.035 12.035 0 01-7.143-7.143c-.162-.441.004-.928.38-1.21l1.293-.97c.363-.271.527-.734.417-1.173L6.963 3.102a1.125 1.125 0 00-1.091-.852H4.5A2.25 2.25 0 002.25 4.5v2.25z" />
              </svg>
              <span>{phoneContact}</span>
            </div>
          </div>

          {/* 4. CHECKS Section */}
          <div className="rounded-xl bg-slate-50 p-4 border border-slate-200/80 space-y-3">
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              CHECKS
            </span>
            <div className="space-y-2.5 text-xs text-slate-700">
              {/* Check 1: Cut-off */}
              <div className="flex items-center gap-2.5">
                <svg className="h-4 w-4 text-emerald-600 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>Before cut-off (4:00 PM) - normal order</span>
              </div>

              {/* Check 2: Weight & Volume */}
              <div className="flex items-center gap-2.5">
                <svg className="h-4 w-4 text-emerald-600 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>Fits a vehicle · ≈ 358 kg · 1.8 m³</span>
              </div>

              {/* Check 3: Fridge Truck */}
              <div className="flex items-center gap-2.5">
                <svg className="h-4 w-4 text-sky-500 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M2 12h20M12 2v20M20 16l-4-4 4-4M4 8l4 4-4 4M16 4l-4 4-4-4M8 20l4-4 4 4" />
                </svg>
                <span>Needs a fridge truck</span>
              </div>

              {/* Check 4: Delivery Window */}
              <div className="flex items-center gap-2.5">
                <svg className="h-4 w-4 text-slate-500 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
                <span>Window 5:15 – 7:45 AM · rear dock</span>
              </div>
            </div>
          </div>
        </form>

        {/* 5. Footer */}
        <div className="flex items-center justify-end gap-3 border-t border-slate-200 bg-white p-4">
          <button
            type="button"
            onClick={onClose}
            className="min-h-[40px] rounded-lg border border-slate-300 bg-white px-5 py-2 text-sm font-semibold text-slate-700 shadow-2xs hover:bg-slate-50 transition"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-lg bg-[#194b83] hover:bg-[#133c6a] px-5 py-2 text-sm font-semibold text-white shadow-xs transition"
          >
            <span>+ Add order</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default AddOrderDrawer;
