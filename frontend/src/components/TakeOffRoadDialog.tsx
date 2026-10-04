import React, { useState } from 'react';

export interface TakeOffRoadDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm?: (details: {
    vehicleId: string;
    reason: string;
    fromDate: string;
    backDate: string;
  }) => void;
  vehicleId?: string;
  vehicleDetails?: string;
}

export const TakeOffRoadDialog: React.FC<TakeOffRoadDialogProps> = ({
  isOpen,
  onClose,
  onConfirm,
  vehicleId = 'VEH006',
  vehicleDetails = 'Truck · fridge · 4,000 kg · 20.0 m³',
}) => {
  const [selectedReason, setSelectedReason] = useState<string>('Workshop');
  const [fromDate] = useState<string>('Thu 1 Oct');
  const [backDate] = useState<string>('Mon 5 Oct (expected)');

  if (!isOpen) return null;

  const reasons = ['Workshop', 'No driver', 'Accident', 'Other'] as const;

  const handleConfirm = () => {
    onConfirm?.({
      vehicleId,
      reason: selectedReason,
      fromDate,
      backDate,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 font-sans">
      {/* 1. Backdrop Overlay */}
      <div
        className="fixed inset-0 bg-slate-900/40 backdrop-blur-2xs transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* 2. Dialog Modal Box */}
      <div className="relative z-10 w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl border border-slate-100 space-y-5 transition-all">
        {/* Header */}
        <div>
          <h2 className="text-xl font-bold tracking-tight text-slate-900">
            Take {vehicleId} off the road?
          </h2>
          <p className="mt-1.5 text-xs sm:text-sm text-slate-500 leading-relaxed">
            {vehicleDetails}. It won't be used in any plan until you put it back.
          </p>
        </div>

        {/* Reason Selector */}
        <div className="space-y-2">
          <label className="text-xs font-bold text-slate-800">Reason</label>
          <div className="flex flex-wrap items-center gap-2">
            {reasons.map((reason) => {
              const isSelected = selectedReason === reason;
              return (
                <button
                  key={reason}
                  type="button"
                  onClick={() => setSelectedReason(reason)}
                  className={`min-h-[36px] rounded-full px-4 py-1.5 text-xs transition-all ${
                    isSelected
                      ? 'bg-[#133c6a] font-semibold text-white shadow-xs'
                      : 'border border-slate-300 bg-white font-medium text-slate-700 hover:border-slate-400 hover:bg-slate-50'
                  }`}
                >
                  {reason}
                </button>
              );
            })}
          </div>
        </div>

        {/* Date Inputs Side-by-Side */}
        <div className="grid grid-cols-2 gap-4">
          {/* "From" Input */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">From</label>
            <div className="flex min-h-[42px] items-center gap-2 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-800 shadow-2xs">
              <svg
                className="h-4 w-4 text-slate-400 flex-shrink-0"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              <span className="truncate">{fromDate}</span>
            </div>
          </div>

          {/* "Back on the road" Input */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Back on the road</label>
            <div className="flex min-h-[42px] items-center gap-2 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-800 shadow-2xs">
              <svg
                className="h-4 w-4 text-slate-400 flex-shrink-0"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              <span className="truncate text-slate-700">{backDate}</span>
            </div>
          </div>
        </div>

        {/* Warning Box */}
        <div className="flex items-start gap-3 rounded-xl border border-amber-300/80 bg-amber-50/70 p-4">
          <svg
            className="h-4 w-4 text-amber-700 flex-shrink-0 mt-0.5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"
            />
          </svg>
          <p className="text-xs text-amber-900 leading-relaxed">
            This is 1 of only 4 available fridge vehicles at Peliyagoda. Thursday's chilled demand is 3× normal - more chilled orders will wait.
          </p>
        </div>

        {/* Footer Actions */}
        <div className="grid grid-cols-2 gap-3 pt-2">
          {/* Critical Take Off Road Button */}
          <button
            type="button"
            onClick={handleConfirm}
            className="inline-flex min-h-[44px] items-center justify-center rounded-lg bg-[#a82222] px-4 py-2.5 text-sm font-semibold text-white shadow-xs transition hover:bg-[#8f1a1a]"
          >
            Take off the road
          </button>

          {/* Cancel Button */}
          <button
            type="button"
            onClick={onClose}
            className="inline-flex min-h-[44px] items-center justify-center rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-2xs transition hover:bg-slate-50"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
};

export default TakeOffRoadDialog;
