import React, { useState } from 'react';

export interface LiveBoardProps {
  onResolveConflict?: (id: string) => void;
  onDecideDelivery?: (id: string) => void;
  onViewRisk?: (id: string) => void;
  onViewShortfall?: (id: string) => void;
}

interface AttentionAlert {
  id: string;
  type: 'conflict' | 'failed' | 'late' | 'shortfall';
  title: string;
  details: string;
  actionText: string;
}

interface TripItem {
  id: string;
  vehicle: string;
  trip: string;
  destination: string;
  progress: string;
  status: 'late-risk' | 'on-the-way' | 'done' | 'offline';
  statusText: string;
}

const DEFAULT_ALERTS: AttentionAlert[] = [
  {
    id: 'alert-1',
    type: 'conflict',
    title: 'Sync conflict',
    details: 'OUT106 · delivered offline by VEH040 at 5:38 AM after you moved it to VEH041',
    actionText: 'Resolve',
  },
  {
    id: 'alert-2',
    type: 'failed',
    title: 'Failed delivery',
    details: 'OUT012 · store closed · 64 chilled cases returning on VEH006',
    actionText: 'Decide',
  },
  {
    id: 'alert-3',
    type: 'late',
    title: 'Late risk',
    details: 'OUT001 · now 7:05–7:35 AM · 38% chance of missing 7:30 AM',
    actionText: 'View',
  },
  {
    id: 'alert-4',
    type: 'shortfall',
    title: 'Shortfall at dock',
    details: 'OUT008 · 2 cases missing from stock · remainder order Fri',
    actionText: 'View',
  },
];

const DEFAULT_TRIPS: TripItem[] = [
  {
    id: 'trip-1',
    vehicle: 'VEH036',
    trip: 'T2',
    destination: 'Colombo',
    progress: '0 of 2 · left 6:39 AM (14 min late)',
    status: 'late-risk',
    statusText: 'Late risk',
  },
  {
    id: 'trip-2',
    vehicle: 'VEH006',
    trip: 'T1',
    destination: 'Gampaha',
    progress: '4 of 6 · on time',
    status: 'on-the-way',
    statusText: 'On the way',
  },
  {
    id: 'trip-3',
    vehicle: 'VEH007',
    trip: 'T2',
    destination: 'Colombo',
    progress: '1 of 3 · on time',
    status: 'on-the-way',
    statusText: 'On the way',
  },
  {
    id: 'trip-4',
    vehicle: 'VEH003',
    trip: 'T1',
    destination: 'Puttalam',
    progress: '1 of 1 · delivered 6:31 AM',
    status: 'done',
    statusText: 'Done',
  },
  {
    id: 'trip-5',
    vehicle: 'VEH040',
    trip: 'T1',
    destination: 'Nuwara Eliya',
    progress: '5 of 5 · synced 6:12 AM',
    status: 'done',
    statusText: 'Done',
  },
  {
    id: 'trip-6',
    vehicle: 'VEH041',
    trip: 'T1',
    destination: 'Badulla',
    progress: '3 of 5 · signal weak',
    status: 'offline',
    statusText: 'Last seen 6:20',
  },
];

export const LiveBoard: React.FC<LiveBoardProps> = ({
  onResolveConflict,
  onDecideDelivery,
  onViewRisk,
  onViewShortfall,
}) => {
  const [alerts] = useState<AttentionAlert[]>(DEFAULT_ALERTS);
  const [trips] = useState<TripItem[]>(DEFAULT_TRIPS);

  const handleAction = (alert: AttentionAlert) => {
    switch (alert.type) {
      case 'conflict':
        onResolveConflict?.(alert.id);
        break;
      case 'failed':
        onDecideDelivery?.(alert.id);
        break;
      case 'late':
        onViewRisk?.(alert.id);
        break;
      case 'shortfall':
        onViewShortfall?.(alert.id);
        break;
    }
  };

  return (
    <div className="space-y-6">
      {/* 1. Top KPI Summary Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* On Time So Far */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
            ON TIME SO FAR
          </div>
          <div className="mt-1 text-3xl font-bold tracking-tight text-emerald-600">
            86%
          </div>
          <div className="mt-1 text-xs text-slate-500">
            49 of 57 completed stops
          </div>
        </div>

        {/* Deferred Today */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
            DEFERRED TODAY
          </div>
          <div className="mt-1 text-3xl font-bold tracking-tight text-rose-600">
            5
          </div>
          <div className="mt-1 text-xs text-slate-500">
            All stores notified at publish
          </div>
        </div>

        {/* Skipped 2+ Runs */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
            SKIPPED 2+ RUNS
          </div>
          <div className="mt-1 text-3xl font-bold tracking-tight text-emerald-600">
            0
          </div>
          <div className="mt-1 text-xs text-slate-500">
            Fairness rule held
          </div>
        </div>

        {/* Fridge-Truck Use */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-4 shadow-2xs">
          <div className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
            FRIDGE-TRUCK USE
          </div>
          <div className="mt-1 text-3xl font-bold tracking-tight text-amber-700">
            92%
          </div>
          <div className="mt-1 text-xs text-slate-500">
            Trips before 8 AM
          </div>
        </div>
      </div>

      {/* 2. Main Two-Column Layout */}
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
        {/* Left Column: Needs attention (~62% width) */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs lg:col-span-7">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-sm font-bold text-slate-900">
              Needs attention ({alerts.length})
            </h2>
            <span className="text-xs font-medium text-slate-400">
              Critical first
            </span>
          </div>

          {/* Alert Cards Stack */}
          <div className="space-y-3">
            {alerts.map((alert) => {
              const isCritical = alert.type === 'conflict' || alert.type === 'failed';

              return (
                <div
                  key={alert.id}
                  className={`flex items-center justify-between gap-4 rounded-xl border p-3.5 transition-colors ${
                    isCritical
                      ? 'border-red-200/80 bg-[#fdf2f2]'
                      : 'border-amber-200/80 bg-[#fffbeb]'
                  }`}
                >
                  {/* Left: Icon & Details */}
                  <div className="flex items-start gap-3">
                    {isCritical ? (
                      <div className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center text-red-600">
                        <svg
                          className="h-4 w-4"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <circle cx="12" cy="12" r="10" />
                          <line x1="15" y1="9" x2="9" y2="15" />
                          <line x1="9" y1="9" x2="15" y2="15" />
                        </svg>
                      </div>
                    ) : (
                      <div className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center text-amber-600">
                        <svg
                          className="h-4 w-4"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
                          />
                        </svg>
                      </div>
                    )}

                    <div>
                      <div
                        className={`text-xs font-bold ${
                          isCritical ? 'text-red-700' : 'text-amber-800'
                        }`}
                      >
                        {alert.title}
                      </div>
                      <div className="mt-0.5 text-xs text-slate-700">
                        {alert.details}
                      </div>
                    </div>
                  </div>

                  {/* Right: Action Button */}
                  <button
                    type="button"
                    onClick={() => handleAction(alert)}
                    className="min-w-[72px] shrink-0 rounded-lg border border-[#183a6b] bg-white px-3.5 py-1.5 text-center text-xs font-bold text-[#183a6b] shadow-2xs transition hover:bg-blue-50 active:scale-[0.98]"
                  >
                    {alert.actionText}
                  </button>
                </div>
              );
            })}
          </div>

          {/* Info Section at bottom */}
          <div className="mt-5 border-t border-slate-100 pt-3.5">
            <div className="mb-2 text-[11px] font-bold text-slate-700">
              Info
            </div>
            <div className="flex items-center gap-2 text-xs text-slate-600">
              <svg
                className="h-4 w-4 shrink-0 text-emerald-600"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <circle cx="12" cy="12" r="10" />
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="m9 12 2 2 4-4"
                />
              </svg>
              <span>
                VEH040 back online at 6:12 AM · 5 stops synced (Nuwara Eliya run)
              </span>
            </div>
          </div>
        </div>

        {/* Right Column: Trips on the road (~38% width) */}
        <div className="rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs lg:col-span-5">
          <h2 className="mb-4 text-sm font-bold text-slate-900">
            Trips on the road
          </h2>

          <div className="divide-y divide-slate-100">
            {trips.map((trip) => (
              <div
                key={trip.id}
                className="flex items-center justify-between py-3.5 first:pt-0 last:pb-0"
              >
                {/* Trip Info */}
                <div>
                  <div className="text-xs font-bold text-slate-900">
                    {trip.vehicle} · {trip.trip} · {trip.destination}
                  </div>
                  <div className="mt-0.5 text-[11px] text-slate-500">
                    {trip.progress}
                  </div>
                </div>

                {/* Status Badge */}
                <div>
                  {trip.status === 'late-risk' && (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 px-2.5 py-1 text-[11px] font-semibold text-amber-800">
                      <svg
                        className="h-3 w-3 shrink-0 text-amber-600"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2.2"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
                        />
                      </svg>
                      {trip.statusText}
                    </span>
                  )}

                  {trip.status === 'on-the-way' && (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-sky-50 px-2.5 py-1 text-[11px] font-semibold text-sky-800">
                      <svg
                        className="h-3 w-3 shrink-0 text-sky-600"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M8.25 18.75a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 0 1-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 0 0-3.213-9.193 2.056 2.056 0 0 0-1.58-.86H14.25M16.5 18.75h-2.25m0-11.25V3.75m0 3.75h-9a1.5 1.5 0 0 0-1.5 1.5v6"
                        />
                      </svg>
                      {trip.statusText}
                    </span>
                  )}

                  {trip.status === 'done' && (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-800">
                      <svg
                        className="h-3 w-3 shrink-0 text-emerald-600"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2.5"
                      >
                        <circle cx="12" cy="12" r="9" />
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="m9 12 2 2 4-4"
                        />
                      </svg>
                      {trip.statusText}
                    </span>
                  )}

                  {trip.status === 'offline' && (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-medium text-slate-600">
                      <svg
                        className="h-3 w-3 shrink-0 text-slate-500"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.773M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88"
                        />
                      </svg>
                      {trip.statusText}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default LiveBoard;
