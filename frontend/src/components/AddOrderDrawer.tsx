// D1b · Add order for an outlet (phone-in)
import React, { useState } from 'react';
import { Button, IconButton } from '../ui/components';
import { PhoneInPartialError, type PhoneInDraft, type PhoneInLine } from '../features/dispatch/core/usePhoneIn';
import { errorMessage } from '../features/dispatch/core/errorMessage';

export interface OutletOption {
  id: string;
  label: string;
}

export interface AddOrderDrawerProps {
  outlets: OutletOption[];
  initialOutletId: string;
  runDateLabel: string;
  ordersClosed: boolean;
  isSubmitting: boolean;
  onSubmit: (draft: PhoneInDraft) => Promise<unknown>;
  onClose: () => void;
}

interface LineDraft {
  temp: PhoneInLine['temp'];
  units: string;
}

const emptyLine = (): LineDraft => ({ temp: 'AMBIENT', units: '' });
const field = 'mt-1 block min-h-11 w-full rounded-lg border border-line bg-surface px-3.5 py-2 text-sm text-ink';
const LINE_OPTIONS: { temp: PhoneInLine['temp']; label: string; icon: string }[] = [
  { temp: 'AMBIENT', label: 'Dry', icon: '▣' },
  { temp: 'CHILLED', label: 'Chilled', icon: '❄' },
];

export const AddOrderDrawer: React.FC<AddOrderDrawerProps> = ({
  outlets,
  initialOutletId,
  runDateLabel,
  ordersClosed,
  isSubmitting,
  onSubmit,
  onClose,
}) => {
  const [outletId, setOutletId] = useState(initialOutletId);
  const [lines, setLines] = useState<LineDraft[]>([emptyLine()]);
  const [note, setNote] = useState('');
  const [error, setError] = useState('');

  const updateLine = (index: number, change: Partial<LineDraft>) =>
    setLines((current) => current.map((line, i) => (i === index ? { ...line, ...change } : line)));

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError('');
    try {
      await onSubmit({
        outletId,
        note,
        lines: lines.map((line) => ({ temp: line.temp, units: Number(line.units) })),
      });
      onClose();
    } catch (failure) {
      if (failure instanceof PhoneInPartialError) setLines((current) => current.slice(failure.createdLines));
      setError(errorMessage(failure));
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end" role="dialog" aria-modal="true" aria-label="Add order for an outlet">
      <button type="button" className="fixed inset-0 bg-overlay" onClick={onClose} aria-label="Close drawer" />

      <form onSubmit={handleSubmit} className="relative z-10 flex h-full w-full max-w-md flex-col bg-surface text-ink shadow-2xl sm:max-w-lg">
        <div className="flex items-start justify-between border-b border-line p-6 pb-4">
          <div>
            <h2 className="text-xl font-bold tracking-tight">Add order for an outlet</h2>
            <p className="mt-1 text-xs text-muted">For stores that phone in. The store gets an in-app notice to check it.</p>
          </div>
          <IconButton label="Close drawer" size="s" type="button" onClick={onClose}>×</IconButton>
        </div>

        <div className="flex-1 space-y-5 overflow-y-auto p-6">
          <label className="block text-xs font-semibold">
            Outlet
            <select className={field} required value={outletId} onChange={(event) => setOutletId(event.target.value)}>
              <option value="" disabled>Choose an outlet</option>
              {outlets.map((outlet) => <option key={outlet.id} value={outlet.id}>{outlet.label}</option>)}
            </select>
          </label>

          <div className="space-y-3">
            <h3 className="text-xs font-semibold">Order lines · {runDateLabel}</h3>
            {lines.map((line, index) => (
              <div key={index} className="space-y-2 rounded-xl border border-line p-3">
                <div className="grid grid-cols-2 gap-3" role="group" aria-label={`Line ${index + 1} type`}>
                  {LINE_OPTIONS.map((option) => (
                    <button key={option.temp} type="button" aria-pressed={line.temp === option.temp} onClick={() => updateLine(index, { temp: option.temp })}
                      className={`flex min-h-11 items-center justify-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium ${line.temp === option.temp ? 'border-2 border-brand bg-brand-soft font-semibold text-brand' : 'border-line bg-surface hover:bg-inset'}`}>
                      <span aria-hidden="true">{option.icon}</span>{option.label}
                    </button>
                  ))}
                </div>
                <div className="flex items-end gap-3">
                  <label className="block flex-1 text-xs font-semibold">
                    Cases
                    <input className={field} type="number" min="1" required value={line.units} onChange={(event) => updateLine(index, { units: event.target.value })} />
                  </label>
                  <Button type="button" tone="outline" size="s" disabled={lines.length === 1 || isSubmitting} aria-label={`Remove line ${index + 1}`} onClick={() => setLines((current) => current.filter((_, i) => i !== index))}>Remove</Button>
                </div>
              </div>
            ))}
            <Button type="button" tone="secondary" size="s" disabled={isSubmitting} onClick={() => setLines((current) => [...current, emptyLine()])}>+ Add line</Button>
          </div>

          <label className="block text-xs font-semibold">
            Note (who phoned, and when)
            <input className={field} maxLength={500} value={note} onChange={(event) => setNote(event.target.value)} />
          </label>

          <div className="space-y-3 rounded-xl border border-line bg-inset p-4">
            <span className="text-[11px] font-bold uppercase tracking-wider text-muted">Checks</span>
            <p className={`flex items-center gap-2.5 text-xs ${ordersClosed ? 'text-status-risk' : 'text-status-delivered'}`}>
              <span aria-hidden="true">{ordersClosed ? '⚠' : '✓'}</span>
              {ordersClosed ? 'After cut-off - late add, tagged "Entered by dispatcher"' : 'Before cut-off - normal order'}
            </p>
            <p className="flex items-center gap-2.5 text-xs text-muted">
              <span aria-hidden="true">⚖</span>
              Weight and volume are estimated by the server from the outlet's past orders.
            </p>
          </div>

          {error && <p role="alert" className="rounded-lg bg-status-failed-soft p-3 text-sm font-semibold text-status-failed">✕ {error}</p>}
        </div>

        <div className="flex items-center justify-end gap-3 border-t border-line bg-surface p-4">
          <Button type="button" tone="outline" size="s" onClick={onClose}>Cancel</Button>
          <Button type="submit" size="s" disabled={isSubmitting || !outletId}>
            {isSubmitting ? 'Creating…' : `+ Add ${lines.length} ${lines.length === 1 ? 'line' : 'lines'}`}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default AddOrderDrawer;
