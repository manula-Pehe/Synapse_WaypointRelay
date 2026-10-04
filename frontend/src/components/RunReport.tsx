import React, { useState } from 'react';

export interface RunReportProps {
  onExportCsv?: () => void;
  onFilterChange?: (filter: string) => void;
}

interface DistrictLateStat {
  district: string;
  countText: string;
  fillPct: number;
  color: 'red' | 'blue';
}

const DISTRICT_STATS: DistrictLateStat[] = [
  {
    district: 'Nuwara Eliya',
    countText: '3 of 5',
    fillPct: 38,
    color: 'red',
  },
  {
    district: 'Badulla',
    countText: '2 of 5',
    fillPct: 24,
    color: 'red',
  },
  {
    district: 'Colombo',
    countText: '2 of 28',
    fillPct: 5,
    color: 'blue',
  },
  {
    district: 'Kalutara',
    countText: '1 of 6',
    fillPct: 10,
    color: 'blue',
  },
  {
    district: 'Galle',
    countText: '1 of 9',
    fillPct: 7,
    color: 'blue',
  },
];

export const RunReport: React.FC<RunReportProps> = ({
  onExportCsv,
  onFilterChange,
}) => {
  const [activeFilter, setActiveFilter] = useState<'today' | 'week' | 'month'>('today');

  const handleFilterClick = (filter: 'today' | 'week' | 'month', label: string) => {
    setActiveFilter(filter);
    onFilterChange?.(label);
  };

  return (
    <div className="space-y-6">
      {/* 1. Top Action Row: Filter Pills on Left, Export on Right */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        {/* Filter Pills */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => handleFilterClick('today', 'Thu 1 Oct')}
            className={`min-h-[36px] rounded-full px-4 py-1.5 text-xs font-semibold transition-all ${
              activeFilter === 'today'
                ? 'bg-[#183a6b] text-white shadow-xs'
                : 'border border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50'
            }`}
          >
            Thu 1 Oct
          </button>

          <button
            type="button"
            onClick={() => handleFilterClick('week', 'This week')}
            className={`min-h-[36px] rounded-full px-4 py-1.5 text-xs font-semibold transition-all ${
              activeFilter === 'week'
                ? 'bg-[#183a6b] text-white shadow-xs'
                : 'border border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50'
            }`}
          >
            This week
          </button>

          <button
            type="button"
            onClick={() => handleFilterClick('month', 'Last 30 days')}
            className={`min-h-[36px] rounded-full px-4 py-1.5 text-xs font-semibold transition-all ${
              activeFilter === 'month'
                ? 'bg-[#183a6b] text-white shadow-xs'
                : 'border border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50'
            }`}
          >
            Last 30 days
          </button>
        </div>

        {/* Export CSV Button */}
        <button
          type="button"
          onClick={onExportCsv}
          className="inline-flex min-h-[36px] items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-1.5 text-xs font-semibold text-slate-800 shadow-2xs transition hover:bg-slate-50 active:scale-[0.98]"
        >
          <svg
            className="h-4 w-4 text-slate-600"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <polyline points="6 9 6 2 18 2 18 9" />
            <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
            <rect x="6" y="14" width="12" height="8" />
          </svg>
          <span>Export CSV</span>
        </button>
      </div>

      {/* 2. KPI Cards (Grid of 5) */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {/* Card 1: ON TIME */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
            ON TIME
          </div>
          <div className="mt-1 text-3xl font-bold tracking-tight text-emerald-600">
            89%
          </div>
          <div className="mt-1 text-xs text-slate-500">
            70 of 79 delivered
          </div>
        </div>

        {/* Card 2: DEFERRED */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
            DEFERRED
          </div>
          <div className="mt-1 text-3xl font-bold tracking-tight text-rose-600">
            5
          </div>
          <div className="mt-1 text-xs text-slate-500">
            1 unavoidable · 4 chosen
          </div>
        </div>

        {/* Card 3: FAILED */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
            FAILED
          </div>
          <div className="mt-1 text-3xl font-bold tracking-tight text-red-700">
            1
          </div>
          <div className="mt-1 text-xs text-slate-500">
            OUT012 · store closed
          </div>
        </div>

        {/* Card 4: PARTIAL */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
            PARTIAL
          </div>
          <div className="mt-1 text-3xl font-bold tracking-tight text-slate-900">
            2
          </div>
          <div className="mt-1 text-xs text-slate-500">
            remainders booked Fri
          </div>
        </div>

        {/* Card 5: SKIPPED 2+ RUNS */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
            SKIPPED 2+ RUNS
          </div>
          <div className="mt-1 text-3xl font-bold tracking-tight text-emerald-600">
            0
          </div>
          <div className="mt-1 text-xs text-slate-500">
            fairness held
          </div>
        </div>
      </div>

      {/* 3. Bottom Two-Column Layout */}
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
        {/* Left Panel: Late deliveries by district (~60% width) */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs lg:col-span-7">
          <h2 className="mb-5 text-sm font-bold text-slate-900">
            Late deliveries by district
          </h2>

          <div className="space-y-4">
            {DISTRICT_STATS.map((item) => (
              <div key={item.district} className="flex items-center gap-4">
                {/* District Name */}
                <span className="w-28 shrink-0 text-xs font-medium text-slate-700">
                  {item.district}
                </span>

                {/* Horizontal Progress Bar */}
                <div className="relative h-3.5 flex-1 overflow-hidden rounded-md bg-slate-100">
                  <div
                    className={`h-full rounded-md transition-all ${
                      item.color === 'red' ? 'bg-[#9e2a2b]' : 'bg-[#183a6b]'
                    }`}
                    style={{ width: `${item.fillPct}%` }}
                  />
                </div>

                {/* Count Ratio */}
                <span className="min-w-[48px] shrink-0 text-right text-xs font-bold text-slate-900">
                  {item.countText}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Right Panel: Exceptions handled (~40% width) */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs lg:col-span-5">
          <h2 className="mb-5 text-sm font-bold text-slate-900">
            Exceptions handled
          </h2>

          <div className="space-y-4 text-xs">
            {/* Sync conflicts */}
            <div className="flex items-start justify-between gap-4">
              <span className="text-slate-500">Sync conflicts</span>
              <span className="text-right text-slate-800">
                <strong className="font-bold text-slate-900">
                  1 · resolved (physical fact kept)
                </strong>
              </span>
            </div>

            {/* Vehicle problems */}
            <div className="flex items-start justify-between gap-4">
              <span className="text-slate-500">Vehicle problems</span>
              <span className="text-right text-slate-800">
                <strong className="font-bold text-slate-900">2</strong> · VEH007 breakdown, VEH036 fridge
              </span>
            </div>

            {/* Plan versions */}
            <div className="flex items-start justify-between gap-4">
              <span className="text-slate-500">Plan versions</span>
              <span className="text-right text-slate-800">
                <strong className="font-bold text-slate-900">v2 · 1 change</strong> after publishing (dock weight)
              </span>
            </div>

            {/* Store issues */}
            <div className="flex items-start justify-between gap-4">
              <span className="text-slate-500">Store issues</span>
              <span className="text-right text-slate-800">
                <strong className="font-bold text-slate-900">2 · 1 resolved</strong>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RunReport;
