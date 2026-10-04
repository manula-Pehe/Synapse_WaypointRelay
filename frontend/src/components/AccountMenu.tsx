import React, { useEffect, useRef } from 'react';

export interface AccountMenuProps {
  isOpen: boolean;
  onClose: () => void;
  user?: {
    name: string;
    email: string;
    role: string;
    depots?: string;
  };
  onSignOut?: () => void;
  onSelectOption?: (option: string) => void;
}

export const AccountMenu: React.FC<AccountMenuProps> = ({
  isOpen,
  onClose,
  user = {
    name: 'Ruwan P.',
    email: 'ruwan.p@waypoint.lk',
    role: 'Central dispatcher',
    depots: 'Peliyagoda · Kandy',
  },
  onSignOut,
  onSelectOption,
}) => {
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu when clicking outside or pressing Escape
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleOptionClick = (option: string) => {
    onSelectOption?.(option);
    onClose();
  };

  const handleSignOut = () => {
    onSignOut?.();
    onClose();
  };

  return (
    <>
      {/* Invisible backdrop to capture outside clicks */}
      <div
        className="fixed inset-0 z-40 bg-transparent"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Floating Popover Container aligned flush with user card below */}
      <div
        ref={menuRef}
        role="menu"
        aria-orientation="vertical"
        className="absolute bottom-full left-0 z-50 mb-3 w-72 overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-xl font-sans transition-all"
      >
        {/* Header Section (Edge-to-edge border) */}
        <div className="border-b border-slate-100 px-4 pt-4 pb-3">
          <div className="text-sm font-bold text-slate-900 leading-tight">
            {user.name}
          </div>
          <div className="mt-1 text-xs text-slate-500 leading-tight">
            {user.email} · {user.role}
          </div>
        </div>

        {/* Menu Items List */}
        <div className="p-1.5 space-y-0.5">
          {/* Depots I plan */}
          <button
            type="button"
            role="menuitem"
            onClick={() => handleOptionClick('depots')}
            className="flex w-full items-start gap-3 rounded-xl px-3 py-2 text-left transition hover:bg-slate-50"
          >
            <svg
              className="mt-0.5 h-4 w-4 shrink-0 text-slate-500"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <rect x="4" y="2" width="16" height="20" rx="2" />
              <line x1="9" y1="22" x2="9" y2="22.01" />
              <line x1="15" y1="22" x2="15" y2="22.01" />
              <line x1="8" y1="6" x2="8.01" y2="6" />
              <line x1="12" y1="6" x2="12.01" y2="6" />
              <line x1="16" y1="6" x2="16.01" y2="6" />
              <line x1="8" y1="10" x2="8.01" y2="10" />
              <line x1="12" y1="10" x2="12.01" y2="10" />
              <line x1="16" y1="10" x2="16.01" y2="10" />
              <line x1="8" y1="14" x2="8.01" y2="14" />
              <line x1="12" y1="14" x2="12.01" y2="14" />
              <line x1="16" y1="14" x2="16.01" y2="14" />
              <line x1="8" y1="18" x2="8.01" y2="18" />
              <line x1="12" y1="18" x2="12.01" y2="18" />
              <line x1="16" y1="18" x2="16.01" y2="18" />
            </svg>
            <div>
              <div className="text-xs font-semibold text-slate-800 leading-tight">
                Depots I plan
              </div>
              <div className="mt-0.5 text-[11px] text-slate-500 leading-tight">
                {user.depots || 'Peliyagoda · Kandy'}
              </div>
            </div>
          </button>

          {/* Language */}
          <button
            type="button"
            role="menuitem"
            onClick={() => handleOptionClick('language')}
            className="flex w-full items-start gap-3 rounded-xl px-3 py-2 text-left transition hover:bg-slate-50"
          >
            <svg
              className="mt-0.5 h-4 w-4 shrink-0 text-slate-500"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="2" y1="12" x2="22" y2="12" />
              <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
            </svg>
            <div>
              <div className="text-xs font-semibold text-slate-800 leading-tight">
                Language
              </div>
              <div className="mt-0.5 text-[11px] text-slate-500 leading-tight">
                English · සිංහල · தமிழ்
              </div>
            </div>
          </button>

          {/* Alerts */}
          <button
            type="button"
            role="menuitem"
            onClick={() => handleOptionClick('alerts')}
            className="flex w-full items-start gap-3 rounded-xl px-3 py-2 text-left transition hover:bg-slate-50"
          >
            <svg
              className="mt-0.5 h-4 w-4 shrink-0 text-slate-500"
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
            <div>
              <div className="text-xs font-semibold text-slate-800 leading-tight">
                Alerts
              </div>
              <div className="mt-0.5 text-[11px] text-slate-500 leading-tight">
                Critical: push + sound
              </div>
            </div>
          </button>
        </div>

        {/* Sign Out Section (Edge-to-edge border) */}
        <div className="border-t border-slate-100 p-1.5">
          <button
            type="button"
            role="menuitem"
            onClick={handleSignOut}
            className="flex w-full items-center gap-3 rounded-xl px-3 py-2 text-left text-xs font-semibold text-red-600 transition hover:bg-red-50/80 active:scale-[0.99]"
          >
            <svg
              className="h-4 w-4 shrink-0 text-red-600"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="19" y1="12" x2="5" y2="12" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
            <span>Sign out</span>
          </button>
        </div>
      </div>
    </>
  );
};

export default AccountMenu;
