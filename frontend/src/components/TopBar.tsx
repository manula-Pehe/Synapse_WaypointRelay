import React, { useState } from 'react';

export interface TopBarProps {
  title?: string;
  subtitle?: string;
  activeDepot?: string;
  onDepotChange?: (depot: string) => void;
  runDate?: string;
  planStatus?: string;
  onNotificationClick?: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  title = 'Order queue · Thu 1 Oct run',
  subtitle = 'Orders closed Wed 4:00 PM · 85 confirmed orders',
  activeDepot: controlledDepot,
  onDepotChange,
  runDate = 'Run: Thu 1 Oct',
  planStatus = 'Plan v1 · not started',
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
        <div className="flex min-h-[40px] items-center gap-1.5 rounded-lg border border-slate-200/80 bg-slate-100/70 px-3 py-1.5 text-xs font-medium text-slate-600">
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
          <span>{planStatus}</span>
        </div>

        {/* Notification Bell Button (40px touch target) */}
        <button
          type="button"
          aria-label="Notifications"
          onClick={onNotificationClick}
          className="flex min-h-[40px] min-w-[40px] items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 shadow-2xs transition hover:bg-slate-50 hover:text-slate-900"
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
    </header>
  );
};

export default TopBar;
