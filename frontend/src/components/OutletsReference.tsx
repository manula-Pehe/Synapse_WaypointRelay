import React, { useState } from 'react';

export interface OutletsReferenceProps {
  onEditDetails?: (outletId: string) => void;
}

interface OutletItem {
  id: string;
  name: string;
  district: string;
  brand: string;
  line: string;
  access: string;
  depot: string;
  deliveryWindow: string;
  receivingContact: string;
  storeManager: string;
  last30Days: string;
}

const OUTLETS_DATA: OutletItem[] = [
  {
    id: 'OUT001',
    name: 'Waypoint Fresh',
    district: 'Colombo',
    brand: 'Fresh',
    line: 'Fresh · rear dock',
    access: 'Rear dock · truck OK',
    depot: 'Peliyagoda',
    deliveryWindow: '5:15 – 7:45 AM',
    receivingContact: 'Sunil · 077 123 4567',
    storeManager: 'Dilani J.',
    last30Days: 'on time 88% · 2 issues',
  },
  {
    id: 'OUT002',
    name: 'Waypoint Fresh',
    district: 'Colombo',
    brand: 'Fresh',
    line: 'Fresh · rear dock',
    access: 'Rear dock · truck OK',
    depot: 'Peliyagoda',
    deliveryWindow: '5:45 – 8:15 AM',
    receivingContact: 'Mahesh · 071 456 7890',
    storeManager: 'K. Perera',
    last30Days: 'on time 92% · 1 issue',
  },
  {
    id: 'OUT003',
    name: 'Waypoint Fresh',
    district: 'Colombo',
    brand: 'Fresh',
    line: 'Fresh · rear dock',
    access: 'Rear dock · truck OK',
    depot: 'Peliyagoda',
    deliveryWindow: '6:15 – 8:45 AM',
    receivingContact: 'Anura · 077 890 1234',
    storeManager: 'S. Jayawardena',
    last30Days: 'on time 95% · 0 issues',
  },
  {
    id: 'OUT008',
    name: 'Waypoint Fresh',
    district: 'Colombo',
    brand: 'Fresh',
    line: 'Fresh · street · van only',
    access: 'Street parking · van only',
    depot: 'Peliyagoda',
    deliveryWindow: '5:15 – 7:15 AM',
    receivingContact: 'Nimal · 077 345 6789',
    storeManager: 'T. Fernando',
    last30Days: 'on time 90% · 2 issues',
  },
  {
    id: 'OUT012',
    name: 'Waypoint Fresh',
    district: 'Colombo',
    brand: 'Fresh',
    line: 'Fresh · street · van only',
    access: 'Street parking · van only',
    depot: 'Peliyagoda',
    deliveryWindow: '5:45 – 7:45 AM',
    receivingContact: 'Prasanna · 076 567 8901',
    storeManager: 'R. Mendis',
    last30Days: 'on time 84% · 3 issues',
  },
  {
    id: 'OUT045',
    name: 'Waypoint Fresh',
    district: 'Kalutara',
    brand: 'Fresh',
    line: 'Fresh · rear dock',
    access: 'Rear dock · truck OK',
    depot: 'Peliyagoda',
    deliveryWindow: '6:15 – 8:45 AM',
    receivingContact: 'Kamal · 075 678 9012',
    storeManager: 'H. De Silva',
    last30Days: 'on time 94% · 1 issue',
  },
  {
    id: 'OUT054',
    name: 'Waypoint Fresh',
    district: 'Galle',
    brand: 'Fresh',
    line: 'Fresh · rear dock',
    access: 'Rear dock · truck OK',
    depot: 'Peliyagoda',
    deliveryWindow: '6:45 – 9:15 AM',
    receivingContact: 'Lalith · 072 789 0123',
    storeManager: 'M. Wickramasinghe',
    last30Days: 'on time 91% · 2 issues',
  },
  {
    id: 'OUT070',
    name: 'Waypoint Style',
    district: 'Kurunegala',
    brand: 'Style',
    line: 'Style · rear dock',
    access: 'Rear dock · truck OK',
    depot: 'Peliyagoda',
    deliveryWindow: '7:15 – 9:45 AM',
    receivingContact: 'Rohan · 078 890 1234',
    storeManager: 'A. Bandara',
    last30Days: 'on time 98% · 0 issues',
  },
];

export const OutletsReference: React.FC<OutletsReferenceProps> = ({
  onEditDetails,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedOutletId, setSelectedOutletId] = useState('OUT001');

  const filteredOutlets = OUTLETS_DATA.filter((outlet) => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      outlet.id.toLowerCase().includes(q) ||
      outlet.district.toLowerCase().includes(q) ||
      outlet.brand.toLowerCase().includes(q) ||
      outlet.name.toLowerCase().includes(q)
    );
  });

  const selectedOutlet =
    OUTLETS_DATA.find((o) => o.id === selectedOutletId) || OUTLETS_DATA[0];

  return (
    <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
      {/* 1. Left Column: Search & Outlet List (~38% width) */}
      <div className="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-2xs lg:col-span-5">
        {/* Search Input Box */}
        <div className="mb-4">
          <label className="mb-1.5 block text-xs font-bold text-slate-800">
            Search
          </label>
          <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-800 shadow-2xs transition focus-within:border-blue-500 focus-within:ring-1 focus-within:ring-blue-500">
            <svg
              className="h-4 w-4 shrink-0 text-slate-400"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <line x1="8" y1="6" x2="21" y2="6" />
              <line x1="8" y1="12" x2="21" y2="12" />
              <line x1="8" y1="18" x2="21" y2="18" />
              <circle cx="4" cy="6" r="1.5" fill="currentColor" />
              <circle cx="4" cy="12" r="1.5" fill="currentColor" />
              <circle cx="4" cy="18" r="1.5" fill="currentColor" />
            </svg>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search outlet, district or brand"
              className="w-full bg-transparent text-xs text-slate-800 placeholder-slate-400 focus:outline-none"
            />
          </div>
        </div>

        {/* Outlets Stacked List */}
        <div className="space-y-1">
          {filteredOutlets.map((outlet) => {
            const isSelected = outlet.id === selectedOutletId;

            return (
              <button
                key={outlet.id}
                type="button"
                onClick={() => setSelectedOutletId(outlet.id)}
                className={`w-full rounded-xl px-3.5 py-2.5 text-left transition ${
                  isSelected
                    ? 'bg-[#e8f0fe] text-blue-900 shadow-2xs'
                    : 'text-slate-800 hover:bg-slate-50'
                }`}
              >
                <div
                  className={`text-xs font-bold ${
                    isSelected ? 'text-[#183a6b]' : 'text-slate-900'
                  }`}
                >
                  {outlet.id} · {outlet.district}
                </div>
                <div className="mt-0.5 text-[11px] text-slate-500">
                  {outlet.line}
                </div>
              </button>
            );
          })}

          {filteredOutlets.length === 0 && (
            <div className="py-6 text-center text-xs text-slate-400">
              No matching outlets found
            </div>
          )}
        </div>
      </div>

      {/* 2. Right Column: Outlet Delivery Rules Reference (~62% width) */}
      <div className="rounded-2xl border border-slate-200/90 bg-white p-6 shadow-2xs lg:col-span-7">
        {/* Header Row */}
        <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-5">
          <h2 className="text-base font-bold text-slate-900 sm:text-lg">
            {selectedOutlet.id} · {selectedOutlet.name} · {selectedOutlet.district}
          </h2>
          <button
            type="button"
            onClick={() => onEditDetails?.(selectedOutlet.id)}
            className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-[#183a6b] bg-white px-3.5 py-1.5 text-xs font-semibold text-[#183a6b] shadow-2xs transition hover:bg-blue-50 active:scale-[0.98]"
          >
            <svg
              className="h-3.5 w-3.5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
            </svg>
            <span>Edit details</span>
          </button>
        </div>

        {/* Details Key-Value List */}
        <div className="mt-5 space-y-4 text-xs">
          {/* Depot */}
          <div className="flex items-center justify-between">
            <span className="text-slate-500">Depot</span>
            <span className="font-semibold text-slate-900">
              {selectedOutlet.depot}
            </span>
          </div>

          {/* Delivery window */}
          <div className="flex items-center justify-between">
            <span className="text-slate-500">Delivery window</span>
            <span className="font-semibold text-slate-900">
              {selectedOutlet.deliveryWindow}
            </span>
          </div>

          {/* Access */}
          <div className="flex items-center justify-between">
            <span className="text-slate-500">Access</span>
            <span className="font-semibold text-slate-900">
              {selectedOutlet.access}
            </span>
          </div>

          {/* Receiving contact */}
          <div className="flex items-center justify-between">
            <span className="text-slate-500">Receiving contact</span>
            <span className="font-semibold text-slate-900">
              {selectedOutlet.receivingContact}
            </span>
          </div>

          {/* Store manager */}
          <div className="flex items-center justify-between">
            <span className="text-slate-500">Store manager</span>
            <span className="font-semibold text-slate-900">
              {selectedOutlet.storeManager}
            </span>
          </div>

          {/* Last 30 days */}
          <div className="flex items-center justify-between">
            <span className="text-slate-500">Last 30 days</span>
            <span className="font-semibold text-slate-900">
              {selectedOutlet.last30Days}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OutletsReference;
