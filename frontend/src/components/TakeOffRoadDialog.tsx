// D2v · Take a vehicle off the road
import React, { useState } from 'react';
import { Button } from '../ui/components';
import { OFF_ROAD_REASONS, type OffRoadReason } from './offRoadReasons';
import { errorMessage } from '../features/dispatch/core/errorMessage';


export interface TakeOffRoadDialogProps {
  vehicleId: string;
  vehicleDetails: string;
  runDateLabel: string;
  fridgeWarning: string | null;
  onConfirm: (reason: OffRoadReason, details: string) => Promise<unknown>;
  onClose: () => void;
}

export const TakeOffRoadDialog: React.FC<TakeOffRoadDialogProps> = ({
  vehicleId,
  vehicleDetails,
  runDateLabel,
  fridgeWarning,
  onConfirm,
  onClose,
}) => {
  const [reason, setReason] = useState<OffRoadReason>('Workshop');
  const [details, setDetails] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');

  const handleConfirm = async () => {
    setIsSaving(true);
    setError('');
    try {
      await onConfirm(reason, details.trim());
      onClose();
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-label={`Take ${vehicleId} off the road`}>
      <button type="button" className="fixed inset-0 bg-overlay" onClick={onClose} aria-label="Close dialog" />

      <div className="relative z-10 w-full max-w-lg space-y-5 rounded-2xl border border-line bg-surface p-6 text-ink shadow-2xl">
        <div>
          <h2 className="text-xl font-bold tracking-tight">Take {vehicleId} off the road?</h2>
          <p className="mt-1.5 text-xs leading-relaxed text-muted sm:text-sm">
            {vehicleDetails}. It won't be used in the plan for {runDateLabel} until you put it back.
          </p>
        </div>

        <div className="space-y-2">
          <span className="text-xs font-bold">Reason</span>
          <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Reason">
            {OFF_ROAD_REASONS.map((option) => (
              <button key={option} type="button" aria-pressed={reason === option} onClick={() => setReason(option)}
                className={`min-h-10 rounded-full px-4 py-1.5 text-xs transition-all ${reason === option ? 'bg-brand font-semibold text-on-brand' : 'border border-line bg-surface font-medium hover:bg-inset'}`}>
                {option}
              </button>
            ))}
          </div>
        </div>

        <label className="block space-y-1.5 text-xs font-semibold">
          Details (optional)
          <input className="block min-h-10 w-full rounded-lg border border-line bg-surface px-3.5 py-2 text-sm font-normal" maxLength={150} value={details} onChange={(event) => setDetails(event.target.value)} />
        </label>

        <div className="grid grid-cols-2 gap-4 text-sm">
          <div className="space-y-1.5"><span className="text-xs font-semibold">From</span><div className="flex min-h-10 items-center rounded-lg border border-line px-3.5 py-2">{runDateLabel}</div></div>
          <div className="space-y-1.5"><span className="text-xs font-semibold">Applies to</span><div className="flex min-h-10 items-center rounded-lg border border-line px-3.5 py-2">This run date only</div></div>
        </div>

        {fridgeWarning && (
          <p className="flex items-start gap-3 rounded-xl border border-status-risk bg-status-risk-soft p-4 text-xs leading-relaxed text-status-risk">
            <span aria-hidden="true">⚠</span>{fridgeWarning}
          </p>
        )}
        {error && <p role="alert" className="rounded-lg bg-status-failed-soft p-3 text-sm font-semibold text-status-failed">✕ {error}</p>}

        <div className="grid grid-cols-2 gap-3 pt-2">
          <Button tone="critical" onClick={handleConfirm} disabled={isSaving}>{isSaving ? 'Saving…' : 'Take off the road'}</Button>
          <Button tone="outline" onClick={onClose} disabled={isSaving}>Cancel</Button>
        </div>
      </div>
    </div>
  );
};

export default TakeOffRoadDialog;
