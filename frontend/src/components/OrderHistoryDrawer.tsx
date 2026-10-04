// D10 · Order history
import React from 'react';
import { IconButton, Spinner } from '../ui/components';
import { OrderStatusBadge } from './OrderStatusBadge';

export type TimelineTone = 'success' | 'brand' | 'warning';

export interface TimelineEvent {
  id: string;
  title: string;
  timestamp: string;
  tone: TimelineTone;
}

export interface OrderHistoryDrawerProps {
  title: string;
  status: string | null;
  events: TimelineEvent[];
  isLoading: boolean;
  errorMessage: string | null;
  onClose: () => void;
}

const DOT_CLASSES: Record<TimelineTone, string> = {
  success: 'bg-success',
  brand: 'bg-brand',
  warning: 'bg-warning',
};

function Timeline({ events }: { events: TimelineEvent[] }) {
  if (events.length === 0) return <p className="text-sm text-muted">No history recorded for this order yet.</p>;
  return (
    <ol className="relative">
      {events.map((event, index) => (
        <li key={event.id} className="relative flex items-start gap-4 pb-5 last:pb-0">
          {index < events.length - 1 && <span className="absolute -bottom-0.5 left-[5px] top-3.5 w-[1.5px] bg-line" aria-hidden="true" />}
          <span className="relative mt-1 flex h-3 w-3 shrink-0 items-center justify-center"><span className={`h-2.5 w-2.5 rounded-full ${DOT_CLASSES[event.tone]}`} /></span>
          <div className="flex flex-col">
            <span className="text-xs font-semibold text-ink">{event.title}</span>
            <span className="mt-0.5 text-[11px] text-muted">{event.timestamp}</span>
          </div>
        </li>
      ))}
    </ol>
  );
}

export const OrderHistoryDrawer: React.FC<OrderHistoryDrawerProps> = ({ title, status, events, isLoading, errorMessage, onClose }) => (
  <div className="fixed inset-0 z-50 flex justify-end" role="dialog" aria-modal="true" aria-label="Order history">
    <button type="button" className="fixed inset-0 bg-overlay" onClick={onClose} aria-label="Close drawer" />
    <div className="relative z-10 flex h-full w-full max-w-md flex-col bg-surface text-ink shadow-2xl sm:max-w-lg">
      <div className="border-b border-line p-6 pb-4">
        <div className="flex items-start justify-between gap-3">
          <h2 className="text-xl font-bold tracking-tight">{title}</h2>
          <IconButton label="Close drawer" size="s" onClick={onClose}>×</IconButton>
        </div>
        {status && <div className="mt-2.5"><OrderStatusBadge status={status} /></div>}
      </div>

      <div className="flex-1 overflow-y-auto p-6">
        <div className="rounded-xl border border-line bg-surface p-5">
          <h3 className="mb-5 text-sm font-bold">History</h3>
          {isLoading && <Spinner label="Loading history…" />}
          {errorMessage && <p role="alert" className="text-sm font-semibold text-status-failed">✕ Could not load the history. {errorMessage}</p>}
          {!isLoading && !errorMessage && <Timeline events={events} />}
        </div>
      </div>
    </div>
  </div>
);

export default OrderHistoryDrawer;
