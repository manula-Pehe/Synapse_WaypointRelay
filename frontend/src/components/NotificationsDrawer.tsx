import React from 'react';

export interface NotificationsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

export const NotificationsDrawer: React.FC<NotificationsDrawerProps> = ({
  isOpen,
  onClose,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden font-sans">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity"
        onClick={onClose}
      />

      <div className="fixed inset-y-0 right-0 flex max-w-full pl-10">
        <div className="w-screen max-w-md border-l border-slate-200 bg-white p-6 shadow-2xl flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <h2 className="text-lg font-bold text-slate-900">Notifications</h2>
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
              >
                <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            <div className="mt-4 space-y-3">
              <div className="rounded-xl border border-rose-100 bg-rose-50/60 p-3.5">
                <div className="flex items-center justify-between text-xs font-bold text-rose-700">
                  <span>Capacity Alert</span>
                  <span>10m ago</span>
                </div>
                <p className="mt-1 text-xs text-slate-700">
                  Shortage of fridge trucks predicted for week 45.
                </p>
              </div>

              <div className="rounded-xl border border-amber-100 bg-amber-50/60 p-3.5">
                <div className="flex items-center justify-between text-xs font-bold text-amber-800">
                  <span>Issue Reported</span>
                  <span>25m ago</span>
                </div>
                <p className="mt-1 text-xs text-slate-700">
                  ISS-0142: 1 damaged yoghurt case reported by OUT001.
                </p>
              </div>

              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5">
                <div className="flex items-center justify-between text-xs font-bold text-slate-700">
                  <span>Run Update</span>
                  <span>1h ago</span>
                </div>
                <p className="mt-1 text-xs text-slate-600">
                  All morning runs dispatched from Peliyagoda.
                </p>
              </div>
            </div>
          </div>

          <div className="border-t border-slate-100 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="w-full rounded-xl bg-[#183a6b] py-2.5 text-xs font-bold text-white transition hover:bg-[#122e54]"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default NotificationsDrawer;
