// D13 · Outlets reference
import React, { useState } from 'react';
import { EmptyState } from '../ui/components';

export interface OutletItem {
  id: string;
  name: string;
  district: string;
  brand: string;
  line: string;
  dock: string;
  access: string;
  depot: string;
  deliveryWindow: string;
}

export interface OutletsReferenceProps {
  outlets: OutletItem[];
}

function matchesSearch(outlet: OutletItem, query: string): boolean {
  const q = query.toLowerCase().trim();
  if (!q) return true;
  return [outlet.id, outlet.district, outlet.brand, outlet.name].some((value) => value.toLowerCase().includes(q));
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <dt className="text-muted">{label}</dt>
      <dd className="font-semibold text-ink">{value}</dd>
    </div>
  );
}

export const OutletsReference: React.FC<OutletsReferenceProps> = ({ outlets }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedOutletId, setSelectedOutletId] = useState<string | null>(null);

  if (outlets.length === 0) return <EmptyState title="No outlets" description="No outlets are listed for this depot." />;

  const filteredOutlets = outlets.filter((outlet) => matchesSearch(outlet, searchQuery));
  const selectedOutlet = outlets.find((outlet) => outlet.id === selectedOutletId) ?? outlets[0];

  return (
    <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
      <div className="rounded-2xl border border-line bg-surface p-4 lg:col-span-5">
        <label className="mb-4 block text-xs font-bold">
          Search
          <input
            type="search"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Search outlet, district or brand"
            className="mt-1.5 block min-h-10 w-full rounded-xl border border-line bg-surface px-3 py-2 text-xs font-normal text-ink focus:border-brand focus:outline-none"
          />
        </label>

        <div className="space-y-1">
          {filteredOutlets.map((outlet) => {
            const isSelected = outlet.id === selectedOutlet.id;
            return (
              <button
                key={outlet.id}
                type="button"
                aria-pressed={isSelected}
                onClick={() => setSelectedOutletId(outlet.id)}
                className={`min-h-10 w-full rounded-xl px-3.5 py-2.5 text-left transition ${isSelected ? 'bg-brand-soft text-brand' : 'text-ink hover:bg-inset'}`}
              >
                <div className="text-xs font-bold">{outlet.id} · {outlet.district}</div>
                <div className="mt-0.5 text-[11px] text-muted">{outlet.line}</div>
              </button>
            );
          })}
          {filteredOutlets.length === 0 && <div className="py-6 text-center text-xs text-muted">No matching outlets found</div>}
        </div>
      </div>

      <div className="rounded-2xl border border-line bg-surface p-6 lg:col-span-7">
        <h2 className="border-b border-line pb-5 text-base font-bold text-ink sm:text-lg">
          {selectedOutlet.id} · {selectedOutlet.brand} · {selectedOutlet.district}
        </h2>
        <dl className="mt-5 space-y-4 text-xs">
          <Detail label="Depot" value={selectedOutlet.depot} />
          <Detail label="Delivery window" value={selectedOutlet.deliveryWindow} />
          <Detail label="Dock" value={selectedOutlet.dock} />
          <Detail label="Access" value={selectedOutlet.access} />
        </dl>
      </div>
    </div>
  );
};

export default OutletsReference;
