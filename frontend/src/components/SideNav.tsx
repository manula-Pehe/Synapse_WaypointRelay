import React, { useState } from 'react';
import AccountMenu from './AccountMenu';

export interface SideNavProps {
  activeItem?: string;
  onItemSelect?: (itemId: string) => void;
  user?: {
    name: string;
    role: string;
    depots: string;
  };
  onSignOut?: () => void;
}

interface NavItem {
  id: string;
  label: string;
  icon: (active: boolean) => React.ReactNode;
}

export const SideNav: React.FC<SideNavProps> = ({
  activeItem: controlledActiveItem,
  onItemSelect,
  onSignOut,
  user = {
    name: 'Ruwan P.',
    role: 'Central dispatch · plans',
    depots: 'Peliyagoda + Kandy',
  },
}) => {
  const [internalActiveItem, setInternalActiveItem] = useState('orders');
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);
  const activeItem = controlledActiveItem ?? internalActiveItem;

  const handleSelect = (id: string) => {
    setInternalActiveItem(id);
    onItemSelect?.(id);
  };

  const navItems: NavItem[] = [
    {
      id: 'orders',
      label: 'Orders',
      icon: (active) => (
        <svg
          className={`h-5 w-5 ${active ? 'text-blue-600' : 'text-slate-500'}`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <line x1="8" y1="6" x2="21" y2="6" />
          <line x1="8" y1="12" x2="21" y2="12" />
          <line x1="8" y1="18" x2="21" y2="18" />
          <line x1="3" y1="6" x2="3.01" y2="6" />
          <line x1="3" y1="12" x2="3.01" y2="12" />
          <line x1="3" y1="18" x2="3.01" y2="18" />
        </svg>
      ),
    },
    {
      id: 'fleet',
      label: 'Fleet',
      icon: (active) => (
        <svg
          className={`h-5 w-5 ${active ? 'text-blue-600' : 'text-slate-500'}`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M5 18H3c-.6 0-1-.4-1-1V9c0-.6.4-1 1-1h10c.6 0 1 .4 1 1v8c0 .6-.4 1-1 1h-2" />
          <path d="M14 9h4l4 4v4c0 .6-.4 1-1 1h-2" />
          <circle cx="7" cy="18" r="2" />
          <circle cx="17" cy="18" r="2" />
        </svg>
      ),
    },
    {
      id: 'plan',
      label: 'Plan',
      icon: (active) => (
        <svg
          className={`h-5 w-5 ${active ? 'text-blue-600' : 'text-slate-500'}`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <rect x="3" y="3" width="7" height="7" rx="1" />
          <rect x="14" y="3" width="7" height="7" rx="1" />
          <rect x="14" y="14" width="7" height="7" rx="1" />
          <rect x="3" y="14" width="7" height="7" rx="1" />
        </svg>
      ),
    },
    {
      id: 'live-board',
      label: 'Live board',
      icon: (active) => (
        <svg
          className={`h-5 w-5 ${active ? 'text-blue-600' : 'text-slate-500'}`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
        </svg>
      ),
    },
    {
      id: 'issues',
      label: 'Issues',
      icon: (active) => (
        <svg
          className={`h-5 w-5 ${active ? 'text-blue-600' : 'text-slate-500'}`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
          <line x1="12" y1="9" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
      ),
    },
    {
      id: 'network-map',
      label: 'Network map',
      icon: (active) => (
        <svg
          className={`h-5 w-5 ${active ? 'text-blue-600' : 'text-slate-500'}`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
          <line x1="8" y1="2" x2="8" y2="18" />
          <line x1="16" y1="6" x2="16" y2="22" />
        </svg>
      ),
    },
    {
      id: 'capacity',
      label: 'Capacity',
      icon: (active) => (
        <svg
          className={`h-5 w-5 ${active ? 'text-blue-600' : 'text-slate-500'}`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <line x1="18" y1="20" x2="18" y2="10" />
          <line x1="12" y1="20" x2="12" y2="4" />
          <line x1="6" y1="20" x2="6" y2="14" />
        </svg>
      ),
    },
    {
      id: 'outlets',
      label: 'Outlets',
      icon: (active) => (
        <svg
          className={`h-5 w-5 ${active ? 'text-blue-600' : 'text-slate-500'}`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
      ),
    },
    {
      id: 'reports',
      label: 'Reports',
      icon: (active) => (
        <svg
          className={`h-5 w-5 ${active ? 'text-blue-600' : 'text-slate-500'}`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <polyline points="6 9 6 2 18 2 18 9" />
          <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
          <rect x="6" y="14" width="12" height="8" />
        </svg>
      ),
    },
  ];

  return (
    <aside className="sticky top-0 hidden md:flex h-screen w-64 flex-shrink-0 flex-col justify-between border-r border-slate-200/90 bg-white">
      <div>
        {/* Brand / Logo Header */}
        <div className="flex items-center gap-3 px-6 py-5">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#0e2a47] text-white shadow-xs">
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
                d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9"
              />
            </svg>
          </div>
          <div className="leading-tight text-left">
            <div className="text-[10px] font-bold tracking-widest text-slate-500 uppercase">
              WAYPOINT
            </div>
            <div className="text-xl font-bold tracking-tight text-slate-900">
              Relay
            </div>
          </div>
        </div>

        {/* Navigation List (40px touch targets) */}
        <nav className="mt-2 space-y-0.5 px-3">
          {navItems.map((item) => {
            const isActive = activeItem === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => handleSelect(item.id)}
                className={`group flex min-h-[40px] w-full items-center gap-3 rounded-lg px-3.5 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-50/80 font-semibold text-blue-600'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                }`}
              >
                <span className="flex-shrink-0">{item.icon(isActive)}</span>
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Dispatcher's Account Menu at Bottom (40px touch target) */}
      <div className="p-3">
        <div className="relative">
          <AccountMenu
            isOpen={isAccountMenuOpen}
            onClose={() => setIsAccountMenuOpen(false)}
            onSignOut={onSignOut}
            user={{
              name: user.name,
              email: 'ruwan.p@waypoint.lk',
              role: user.role,
              depots: user.depots,
            }}
          />
          <button
            type="button"
            onClick={() => setIsAccountMenuOpen((prev) => !prev)}
            className="flex min-h-[40px] w-full flex-col items-start rounded-xl bg-slate-100/80 p-3 text-left transition hover:bg-slate-200/70"
          >
            <span className="text-sm font-bold text-slate-900">{user.name}</span>
            <span className="mt-0.5 text-xs text-slate-500">{user.role}</span>
            <span className="text-xs text-slate-500">{user.depots}</span>
          </button>
        </div>
      </div>
    </aside>
  );
};

export default SideNav;
