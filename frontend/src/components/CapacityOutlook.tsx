// D7 · Capacity outlook
import React, { useState } from 'react';

export interface CapacityOutlookProps {
  onApplySuggestedMoves?: () => void;
  onDepotChange?: (depot: string) => void;
  onNotificationClick?: () => void;
}

interface WeekForecast {
  week: string;
  need: number;
  available: number;
  isOverCapacity: boolean;
  minRange: number;
  maxRange: number;
  isPeakEvent?: boolean;
  peakLabel?: string;
}

const FORECAST_DATA: WeekForecast[] = [
  { week: 'wk 41', need: 7.5, available: 9, isOverCapacity: false, minRange: 6.8, maxRange: 8.3 },
  { week: 'wk 42', need: 7.8, available: 9, isOverCapacity: false, minRange: 7.1, maxRange: 8.6 },
  { week: 'wk 43', need: 8.1, available: 9, isOverCapacity: false, minRange: 7.4, maxRange: 8.9 },
  { week: 'wk 44', need: 9.4, available: 9, isOverCapacity: true, minRange: 8.5, maxRange: 10.3 },
  { week: 'wk 45', need: 11.2, available: 7, isOverCapacity: true, minRange: 9.8, maxRange: 12.3, isPeakEvent: true, peakLabel: 'Deepavali' },
  { week: 'wk 46', need: 9.0, available: 9, isOverCapacity: false, minRange: 8.1, maxRange: 9.9 },
  { week: 'wk 47', need: 7.8, available: 9, isOverCapacity: false, minRange: 7.0, maxRange: 8.7 },
  { week: 'wk 48', need: 7.6, available: 9, isOverCapacity: false, minRange: 6.9, maxRange: 8.4 },
  { week: 'wk 49', need: 8.3, available: 8, isOverCapacity: true, minRange: 7.5, maxRange: 9.2 },
  { week: 'wk 50', need: 8.0, available: 9, isOverCapacity: false, minRange: 7.2, maxRange: 8.8 },
];

export const CapacityOutlook: React.FC<CapacityOutlookProps> = ({
  onApplySuggestedMoves,
  onDepotChange,
  onNotificationClick,
}) => {
  const [selectedDepot, setSelectedDepot] = useState('Peliyagoda');
  const [applied, setApplied] = useState(false);

  const handleDepotSelect = (depot: string) => {
    setSelectedDepot(depot);
    onDepotChange?.(depot);
  };

  const handleApplyMoves = () => {
    setApplied(true);
    onApplySuggestedMoves?.();
  };

  const chartHeight = 220;
  const maxValue = 13; // Max scale for Y axis

  // Convert value (0 to 13) to SVG Y coordinate
  const getY = (val: number) => {
    return chartHeight - (val / maxValue) * (chartHeight - 30);
  };

  return (
    <div className="space-y-6 font-sans">
      <div role="status" className="flex items-center gap-3 rounded-xl border border-status-risk bg-status-risk-soft p-4 text-sm font-semibold text-status-risk">
        <span aria-hidden="true">⚠</span>
        Sample preview — not connected to live data
      </div>
      {/* 1. Top Header Area */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200/80 pb-4">
        {/* Title & Subtitle */}
        <div>
          <h1 className="text-xl font-bold tracking-tight text-slate-900 sm:text-2xl">
            Capacity outlook · next 10 weeks
          </h1>
          <p className="mt-0.5 text-xs text-slate-500 sm:text-sm">
            Demand forecast vs fleet · Peliyagoda · plan fridge trucks before peaks
          </p>
        </div>

        {/* Right side controls */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Depot toggle buttons */}
          <div className="flex min-h-[38px] items-center rounded-lg bg-slate-100 p-1">
            {['Peliyagoda', 'Kandy', 'All depots'].map((depot) => {
              const isSelected = selectedDepot === depot;
              return (
                <button
                  key={depot}
                  type="button"
                  onClick={() => handleDepotSelect(depot)}
                  className={`min-h-[30px] rounded-md px-3 text-xs font-medium transition-all ${
                    isSelected
                      ? 'bg-white text-slate-900 shadow-2xs font-semibold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {depot}
                </button>
              );
            })}
          </div>

          {/* Badge: "Estimated · rule-based" (US-10.1 requirement) */}
          <div className="flex min-h-[38px] items-center gap-1.5 rounded-lg border border-blue-200/90 bg-blue-50/80 px-3 py-1.5 text-xs font-semibold text-blue-700 shadow-2xs">
            <svg
              className="h-3.5 w-3.5 text-blue-600"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1-2.5-2.5Z" />
              <path d="M6 6h10" />
              <path d="M6 10h10" />
              <path d="M6 14h7" />
            </svg>
            <span>Estimated · rule-based</span>
          </div>

          {/* Notification Bell */}
          <button
            type="button"
            aria-label="Notifications"
            onClick={onNotificationClick}
            className="flex min-h-[38px] min-w-[38px] items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 shadow-2xs transition hover:bg-slate-50 hover:text-slate-900"
          >
            <svg
              className="h-4 w-4"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" />
            </svg>
          </button>
        </div>
      </div>

      {/* 2. Top KPI Cards (Grid of 4) */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Card 1: FRIDGE TRUCKS */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              FRIDGE TRUCKS
            </span>
            <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-0.5 text-[11px] font-semibold text-rose-700">
              <svg className="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="15" y1="9" x2="9" y2="15" />
                <line x1="9" y1="9" x2="15" y2="15" />
              </svg>
              Short in wk 45
            </span>
          </div>
          <p className="mt-2 text-xs font-medium text-slate-800">
            Need ~11 a day, 7 planned
          </p>
        </div>

        {/* Card 2: DRY TRUCKS */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              DRY TRUCKS
            </span>
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-[11px] font-semibold text-emerald-700">
              <svg className="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="12" cy="12" r="9" />
                <path strokeLinecap="round" strokeLinejoin="round" d="m9 12 2 2 4-4" />
              </svg>
              OK
            </span>
          </div>
          <p className="mt-2 text-xs font-medium text-slate-800">
            Peak use 68%
          </p>
        </div>

        {/* Card 3: VANS */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              VANS
            </span>
            <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-[11px] font-semibold text-amber-800">
              <svg className="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
              </svg>
              Tight
            </span>
          </div>
          <p className="mt-2 text-xs font-medium text-slate-800">
            Van-only chilled rises in wk 45
          </p>
        </div>

        {/* Card 4: DRIVERS */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              DRIVERS
            </span>
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-[11px] font-semibold text-emerald-700">
              <svg className="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="12" cy="12" r="9" />
                <path strokeLinecap="round" strokeLinejoin="round" d="m9 12 2 2 4-4" />
              </svg>
              OK
            </span>
          </div>
          <p className="mt-2 text-xs font-medium text-slate-800">
            1 driver per vehicle on the road
          </p>
        </div>
      </div>

      {/* 3. Main Two-Column Layout */}
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
        {/* Left Column: Chart Area (~65% width) */}
        <div className="rounded-2xl border border-slate-200/90 bg-white p-6 shadow-2xs lg:col-span-8">
          <h2 className="text-sm font-bold text-slate-900 sm:text-base">
            Fridge-truck days needed per day vs available
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            Forecast (bar, with range) vs fridge vehicles available after planned workshop time (dashed). Deepavali week marked.
          </p>

          {/* Bar Chart Container */}
          <div className="relative mt-8 select-none">
            {/* Y-Axis Labels & Horizontal Grid Lines */}
            <div className="relative h-[220px] w-full">
              {[12, 8, 4, 0].map((tick) => {
                const yPos = getY(tick);
                return (
                  <div
                    key={tick}
                    className="absolute left-0 right-0 flex items-center"
                    style={{ top: `${yPos}px` }}
                  >
                    <span className="w-6 shrink-0 text-right text-[11px] font-medium text-slate-400">
                      {tick}
                    </span>
                    <div className="ml-3 h-[1px] flex-1 bg-slate-100" />
                  </div>
                );
              })}

              {/* Bars + Overlay Grid */}
              <div className="absolute inset-y-0 left-9 right-2 flex items-end justify-between px-1">
                {FORECAST_DATA.map((item) => {
                  const barHeight = ((item.need / maxValue) * (chartHeight - 30));
                  const rangeTop = getY(item.maxRange);
                  const rangeBottom = getY(item.minRange);

                  return (
                    <div
                      key={item.week}
                      className="group relative flex h-full flex-1 flex-col items-center justify-end px-1"
                    >
                      {/* Deepavali Label above wk 45 */}
                      {item.isPeakEvent && (
                        <div
                          className="absolute z-20 text-[11px] font-bold text-red-700"
                          style={{ top: `${rangeTop - 22}px` }}
                        >
                          {item.peakLabel}
                        </div>
                      )}

                      {/* Error bar (Forecast Range) */}
                      <div
                        className="absolute z-10 w-[1.5px] bg-slate-900 pointer-events-none"
                        style={{
                          top: `${rangeTop}px`,
                          height: `${Math.max(rangeBottom - rangeTop, 2)}px`,
                        }}
                      >
                        {/* Top cap */}
                        <div className="absolute -left-[3px] top-0 h-[1.5px] w-[7px] bg-slate-900" />
                        {/* Bottom cap */}
                        <div className="absolute -left-[3px] bottom-0 h-[1.5px] w-[7px] bg-slate-900" />
                      </div>

                      {/* Bar Element */}
                      <div
                        className={`w-full max-w-[28px] rounded-t-xs transition-all shadow-xs ${
                          item.isOverCapacity
                            ? 'bg-[#a82424] hover:bg-[#8f1a1a]'
                            : 'bg-[#3b82f6] hover:bg-[#2563eb]'
                        }`}
                        style={{ height: `${barHeight}px` }}
                      />
                    </div>
                  );
                })}
              </div>

              {/* Dashed Line Overlay for Fridge Vehicles Available */}
              <svg
                className="absolute inset-0 left-9 right-2 h-full w-[calc(100%-2.25rem)] pointer-events-none"
                viewBox="0 0 1000 220"
                preserveAspectRatio="none"
              >
                {/*
                  10 weeks positioned at x = 50, 150, 250, 350, 450, 550, 650, 750, 850, 950
                  wk 41-44: 9 avail -> getY(9)
                  wk 45: 7 avail -> getY(7)
                  wk 46-48: 9 avail -> getY(9)
                  wk 49: 8 avail -> getY(8)
                  wk 50: 9 avail -> getY(9)
                */}
                <path
                  d={`
                    M 0 ${getY(9)}
                    L 350 ${getY(9)}
                    L 450 ${getY(7)}
                    L 550 ${getY(9)}
                    L 750 ${getY(9)}
                    L 850 ${getY(8)}
                    L 950 ${getY(9)}
                    L 1000 ${getY(9)}
                  `}
                  fill="none"
                  stroke="#b45309"
                  strokeWidth="2.5"
                  strokeDasharray="6 4"
                />
              </svg>
            </div>

            {/* X-Axis Labels */}
            <div className="mt-2 flex items-center justify-between pl-9 pr-2">
              {FORECAST_DATA.map((item) => (
                <div
                  key={item.week}
                  className="flex-1 text-center text-[11px] font-medium text-slate-500"
                >
                  {item.week}
                </div>
              ))}
            </div>
          </div>

          {/* Legend */}
          <div className="mt-6 flex flex-wrap items-center gap-5 border-t border-slate-100 pt-4 text-xs text-slate-600">
            {/* Forecast need */}
            <div className="flex items-center gap-2">
              <span className="h-3 w-3 rounded-2xs bg-[#3b82f6]" />
              <span>Forecast need</span>
            </div>

            {/* Need above available */}
            <div className="flex items-center gap-2">
              <span className="h-3 w-3 rounded-2xs bg-[#a82424]" />
              <span>Need above available</span>
            </div>

            {/* Fridge vehicles available */}
            <div className="flex items-center gap-2">
              <span className="inline-block w-5 border-t-2 border-dashed border-[#b45309]" />
              <span>Fridge vehicles available</span>
            </div>
          </div>

          {/* Explanatory Footer Note */}
          <p className="mt-3 text-[11px] leading-relaxed text-slate-400">
            Why fridge-truck days, not m³: historically fridge trucks leave only ~25% full — trips before 8 AM run out first, not space. Values illustrative; generated from the Task 2A forecast.
          </p>
        </div>

        {/* Right Column: Action Panels (~35% width) */}
        <div className="space-y-5 lg:col-span-4">
          {/* Panel 1: Recommendations */}
          <div className="rounded-2xl border-2 border-[#183a6b] bg-white p-5 shadow-xs">
            {/* Header */}
            <div className="mb-4 flex items-center gap-2">
              <div className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-50 text-[#183a6b]">
                <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <circle cx="12" cy="12" r="10" />
                  <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
              </div>
              <h3 className="text-sm font-bold text-slate-900">
                Recommendations · wk 45
              </h3>
            </div>

            {/* Recommendations List */}
            <div className="space-y-4 text-xs">
              {/* Item 1 */}
              <div className="flex items-start gap-3">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-blue-50 text-[11px] font-bold text-blue-700">
                  1
                </span>
                <div>
                  <div className="font-bold text-slate-900 leading-snug">
                    Move VEH004 and VEH005 service to wk 47
                  </div>
                  <div className="mt-0.5 text-[11px] text-slate-500">
                    +2 fridge trucks in the peak week · no cost
                  </div>
                </div>
              </div>

              {/* Item 2 */}
              <div className="flex items-start gap-3">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-blue-50 text-[11px] font-bold text-blue-700">
                  2
                </span>
                <div>
                  <div className="font-bold text-slate-900 leading-snug">
                    Plan second pre-dawn trips for Colombo and Gampaha
                  </div>
                  <div className="mt-0.5 text-[11px] text-slate-500">
                    Short drives leave room for 2 trips before 8 AM
                  </div>
                </div>
              </div>

              {/* Item 3 */}
              <div className="flex items-start gap-3">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-blue-50 text-[11px] font-bold text-blue-700">
                  3
                </span>
                <div>
                  <div className="font-bold text-slate-900 leading-snug">
                    Ask Galle and Matara stores to order chilled one day earlier
                  </div>
                  <div className="mt-0.5 text-[11px] text-slate-500">
                    Spreads far-district demand across the week
                  </div>
                </div>
              </div>

              {/* Item 4 */}
              <div className="flex items-start gap-3">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-blue-50 text-[11px] font-bold text-blue-700">
                  4
                </span>
                <div>
                  <div className="font-bold text-slate-900 leading-snug">
                    Hire 1 fridge truck Wed–Fri if still short
                  </div>
                  <div className="mt-0.5 text-[11px] text-slate-500">
                    Last resort · check on Mon of wk 44
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Panel 2: Workshop planner */}
          <div className="rounded-2xl border border-slate-200/90 bg-white p-5 shadow-xs">
            {/* Header */}
            <div className="mb-4 flex items-center gap-2">
              <svg
                className="h-4 w-4 text-slate-700"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
              </svg>
              <h3 className="text-sm font-bold text-slate-900">
                Workshop planner
              </h3>
            </div>

            {/* Vehicle List */}
            <div className="space-y-2 text-xs">
              <div className="text-slate-700">
                <strong className="font-bold text-slate-900">VEH004</strong>{' '}
                <span className="text-slate-600">Service wk 45 → suggest wk 47</span>
              </div>
              <div className="text-slate-700">
                <strong className="font-bold text-slate-900">VEH005</strong>{' '}
                <span className="text-slate-600">Service wk 45 → suggest wk 47</span>
              </div>
              <div className="text-slate-700">
                <strong className="font-bold text-slate-900">VEH002</strong>{' '}
                <span className="text-slate-600">Service wk 49 · OK</span>
              </div>
            </div>

            {/* Action Button */}
            <button
              type="button"
              onClick={handleApplyMoves}
              disabled={applied}
              className={`mt-5 w-full rounded-xl py-2.5 text-xs font-bold transition shadow-xs ${
                applied
                  ? 'bg-emerald-600 text-white cursor-default'
                  : 'bg-[#183a6b] text-white hover:bg-[#122e54] active:scale-[0.99]'
              }`}
            >
              {applied ? '✓ Moves applied' : 'Apply suggested moves'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CapacityOutlook;
