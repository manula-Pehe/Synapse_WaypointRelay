import React, { useState } from 'react';

export interface MobileLiveBoardProps {
  onNotificationClick?: () => void;
  onExceptionClick?: (id: string) => void;
  onTripClick?: (id: string) => void;
}

interface ExceptionItem {
  id: string;
  type: 'conflict' | 'failed' | 'late' | 'shortfall';
  title: string;
  subtext: string;
}

interface MobileTripItem {
  id: string;
  name: string;
  badgeType: 'late-risk' | 'progress' | 'done';
  badgeText: string;
}

const DEFAULT_EXCEPTIONS: ExceptionItem[] = [
  {
    id: 'exc-1',
    type: 'conflict',
    title: 'Sync conflict',
    subtext: 'OUT106 · delivered offline',
  },
  {
    id: 'exc-2',
    type: 'failed',
    title: 'Failed delivery',
    subtext: 'OUT012 · store closed',
  },
  {
    id: 'exc-3',
    type: 'late',
    title: 'Late risk',
    subtext: 'OUT001 · 38% · 7:05–7:35',
  },
  {
    id: 'exc-4',
    type: 'shortfall',
    title: 'Shortfall',
    subtext: 'OUT008 · 2 cases',
  },
];

const DEFAULT_TRIPS: MobileTripItem[] = [
  {
    id: 'trip-1',
    name: 'VEH036 · T2 · Colombo',
    badgeType: 'late-risk',
    badgeText: 'Late risk',
  },
  {
    id: 'trip-2',
    name: 'VEH006 · T1 · Gampaha',
    badgeType: 'progress',
    badgeText: '4 of 6',
  },
  {
    id: 'trip-3',
    name: 'VEH040 · T1 · Nuwara Eliya',
    badgeType: 'done',
    badgeText: 'Done',
  },
];

export const MobileLiveBoard: React.FC<MobileLiveBoardProps> = ({
  onNotificationClick,
  onExceptionClick,
  onTripClick,
}) => {
  const [activeTab, setActiveTab] = useState<'live' | 'alerts'>('live');

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-gray-50 text-slate-800 antialiased">
      {/* Header Area: Clean title row sitting naturally at the top with compact pt-4 padding */}
      <div className="shrink-0 border-b border-gray-100 bg-white px-4 pt-4 pb-3">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Live · 6:45 AM
          </h1>
          <button
            type="button"
            aria-label="Notifications"
            onClick={onNotificationClick}
            className="flex h-9 w-9 items-center justify-center rounded-full text-slate-900 transition hover:bg-slate-100 active:scale-95"
          >
            <svg
              className="h-6 w-6"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
            >
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" />
            </svg>
          </button>
        </div>
        <p className="mt-0.5 text-xs text-slate-500">
          All depots · 86% on time
        </p>
      </div>

      {/* Scrollable Content Area */}
      <div className="flex-1 space-y-5 overflow-y-auto px-4 pt-4 pb-24">
        {/* Exceptions List */}
        <div className="space-y-2.5">
          {DEFAULT_EXCEPTIONS.map((item) => {
            const isCritical = item.type === 'conflict' || item.type === 'failed';

            return (
              <button
                key={item.id}
                type="button"
                onClick={() => onExceptionClick?.(item.id)}
                className={`flex w-full items-center justify-between rounded-2xl p-4 text-left transition active:scale-[0.99] ${
                  isCritical
                    ? 'bg-[#fef2f2] hover:bg-[#fee2e2]/70'
                    : 'bg-[#fffbeb] hover:bg-[#fef3c7]/70'
                }`}
              >
                {/* Left Icon + Text */}
                <div className="flex items-center gap-3.5">
                  {isCritical ? (
                    <div className="flex h-5 w-5 shrink-0 items-center justify-center text-red-600">
                      <svg
                        className="h-5 w-5"
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
                    <div className="flex h-5 w-5 shrink-0 items-center justify-center text-amber-700">
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
                          d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
                        />
                      </svg>
                    </div>
                  )}

                  <div>
                    <div
                      className={`text-sm font-bold ${
                        isCritical ? 'text-red-700' : 'text-amber-800'
                      }`}
                    >
                      {item.title}
                    </div>
                    <div className="mt-0.5 text-xs text-slate-800">
                      {item.subtext}
                    </div>
                  </div>
                </div>

                {/* Right Chevron */}
                <svg
                  className="h-4 w-4 shrink-0 text-slate-500"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              </button>
            );
          })}
        </div>

        {/* Trips on the road */}
        <div className="pt-2">
          <h2 className="mb-3.5 text-sm font-bold text-slate-900">
            Trips on the road
          </h2>

          <div className="space-y-3.5">
            {DEFAULT_TRIPS.map((trip) => (
              <div
                key={trip.id}
                onClick={() => onTripClick?.(trip.id)}
                className="flex cursor-pointer items-center justify-between rounded-lg py-1 transition hover:bg-slate-100/60"
              >
                <span className="text-xs font-bold text-slate-900">
                  {trip.name}
                </span>

                {/* Status Badges */}
                {trip.badgeType === 'late-risk' && (
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-[#fef3c7] px-2.5 py-1 text-xs font-semibold text-[#92400e]">
                    <svg
                      className="h-3 w-3 shrink-0 text-amber-700"
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
                    {trip.badgeText}
                  </span>
                )}

                {trip.badgeType === 'progress' && (
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-[#e0f2fe] px-2.5 py-1 text-xs font-semibold text-[#0369a1]">
                    <svg
                      className="h-3.5 w-3.5 shrink-0 text-sky-700"
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
                    {trip.badgeText}
                  </span>
                )}

                {trip.badgeType === 'done' && (
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-[#dcfce7] px-2.5 py-1 text-xs font-semibold text-[#15803d]">
                    <svg
                      className="h-3.5 w-3.5 shrink-0 text-emerald-700"
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
                    {trip.badgeText}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom Navigation Bar */}
      <nav className="fixed bottom-0 left-0 z-50 flex w-full items-center border-t border-gray-200 bg-white py-2 shadow-xs">
        {/* Live Tab */}
        <button
          type="button"
          onClick={() => setActiveTab('live')}
          className={`flex flex-1 flex-col items-center justify-center gap-1 py-1 text-xs font-medium transition ${
            activeTab === 'live'
              ? 'font-semibold text-blue-600'
              : 'text-slate-500 hover:text-slate-900'
          }`}
        >
          <svg
            className="h-5 w-5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
          <span>Live</span>
        </button>

        {/* Alerts Tab */}
        <button
          type="button"
          onClick={() => setActiveTab('alerts')}
          className={`flex flex-1 flex-col items-center justify-center gap-1 py-1 text-xs font-medium transition ${
            activeTab === 'alerts'
              ? 'font-semibold text-blue-600'
              : 'text-slate-500 hover:text-slate-900'
          }`}
        >
          <svg
            className="h-5 w-5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
          <span>Alerts</span>
        </button>
      </nav>
    </div>
  );
};

export default MobileLiveBoard;
