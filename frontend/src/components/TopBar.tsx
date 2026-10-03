import React, { useState } from 'react';

export interface TopBarProps {
  title?: string;
  subtitle?: string;
  activeDepot?: string;
  onDepotChange?: (depot: string) => void;
  runDate?: string;
  planStatus?: string;
  notificationCount?: number;
  onNotificationClick?: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  title = 'Order queue · Thu 1 Oct run',
  subtitle = 'Orders closed Wed 4:00 PM · 85 confirmed orders',
  activeDepot: controlledDepot,
  onDepotChange,
  runDate = 'Run: Thu 1 Oct',
  planStatus = 'Plan v1 · not started',
  notificationCount = 8,
  onNotificationClick,
}) => {
  const [internalDepot, setInternalDepot] = useState('Peliyagoda');
  const activeDepot = controlledDepot ?? internalDepot;

  const handleDepotSelect = (depot: string) => {
    setInternalDepot(depot);
    onDepotChange?.(depot);
  };

  const depots = ['Peliyagoda', 'Kandy', 'All depots'] as const;

  return (
    <header className="sticky top-0 z-20 flex min-h-[72px] flex-wrap items-center justify-between gap-4 border-b border-slate-200/90 bg-white px-8 py-4">
      {/* Title & Subtitle Area */}
      <div>
        <h1 className="text-xl font-bold tracking-tight text-slate-900 sm:text-2xl">
          {title}
        </h1>
        <p className="mt-0.5 text-xs text-slate-500 sm:text-sm">{subtitle}</p>
      </div>

      {/* Top Bar Controls & Badges */}
      <div className="flex flex-wrap items-center gap-2.5">
        {/* Depot Switch Buttons Segmented Group (40px touch targets) */}
        <div className="flex min-h-[40px] items-center rounded-lg bg-slate-100 p-1">
          {depots.map((depot) => {
            const isSelected = activeDepot === depot;
            return (
              <button
                key={depot}
                type="button"
                onClick={() => handleDepotSelect(depot)}
                className={`min-h-[32px] rounded-md px-3 text-xs font-medium transition-all ${
                  isSelected
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {depot}
              </button>
            );
          })}
        </div>

        {/* "Run: Thu 1 Oct" Date Pill */}
        <div className="flex min-h-[40px] items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-2xs">
          <svg
            className="h-4 w-4 text-slate-500"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <span>{runDate}</span>
        </div>

        {/* Plan Status Badge */}
        {planStatus && (
          <div
            className={`flex min-h-[40px] items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs ${
              planStatus.includes('open')
                ? 'border border-amber-200/90 bg-amber-50/80 font-semibold text-amber-800'
                : 'border border-slate-200/80 bg-slate-100/70 font-medium text-slate-600'
            }`}
          >
            {planStatus.includes('open') ? (
              <svg
                className="h-3.5 w-3.5 text-amber-600"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
              </svg>
            ) : (
              <svg
                className="h-3.5 w-3.5 text-slate-500"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <path d="M15 14l-3-3m0 0l-3-3m3 3l3-3m-3 3l-3 3" />
                <circle cx="12" cy="12" r="9" />
              </svg>
            )}
            <span>{planStatus}</span>
          </div>
        )}

        {/* Notification Bell Button (40px touch target) */}
        <button
          type="button"
          aria-label="Notifications"
          onClick={onNotificationClick}
          className="relative flex min-h-[40px] min-w-[40px] items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 shadow-2xs transition hover:bg-slate-50 hover:text-slate-900"
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
          {notificationCount !== undefined && notificationCount > 0 && (
            <span className="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full border border-white bg-red-600 text-[10px] font-bold text-white shadow-xs">
              {notificationCount}
            </span>
          )}
        </button>
      </div>
    </header>
  );
};

export default TopBar;
