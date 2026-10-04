import React, { useState } from 'react';

export interface IssueItem {
  id: string;
  from: string;
  what: string;
  received: string;
  status: 'Needs reply' | 'Decision needed' | 'Auto-handled';
  category: 'needs-you' | 'auto-handled';
}

export interface IssuesInboxProps {
  onIssueSelect?: (issue: IssueItem) => void;
  onFilterChange?: (filter: 'all' | 'needs-you' | 'auto-handled') => void;
}

const ISSUES_DATA: IssueItem[] = [
  {
    id: 'ISS-0142',
    from: 'OUT001 · store',
    what: 'Damaged · 1 case · asks for replacement',
    received: '7:44 AM',
    status: 'Needs reply',
    category: 'needs-you',
  },
  {
    id: 'ISS-0144',
    from: 'OUT012 · driver VEH006',
    what: 'Delivery failed · store closed · 64 cases returning',
    received: '6:02 AM',
    status: 'Decision needed',
    category: 'needs-you',
  },
  {
    id: 'ISS-0143',
    from: 'OUT008 · dock',
    what: 'Short · 2 cases · remainder order created',
    received: '4:16 AM',
    status: 'Auto-handled',
    category: 'auto-handled',
  },
];

export const IssuesInbox: React.FC<IssuesInboxProps> = ({
  onIssueSelect,
  onFilterChange,
}) => {
  const [activeFilter, setActiveFilter] = useState<'all' | 'needs-you' | 'auto-handled'>('all');

  const handleFilterClick = (filter: 'all' | 'needs-you' | 'auto-handled') => {
    setActiveFilter(filter);
    onFilterChange?.(filter);
  };

  const filteredIssues = ISSUES_DATA.filter((issue) => {
    if (activeFilter === 'all') return true;
    return issue.category === activeFilter;
  });

  return (
    <div className="space-y-4 font-sans">
      {/* 1. Top Filters Row */}
      <div className="flex flex-wrap items-center gap-2">
        {/* Button 1: All 5 */}
        <button
          type="button"
          onClick={() => handleFilterClick('all')}
          className={`min-h-[34px] rounded-full px-4 text-xs font-semibold transition-all ${
            activeFilter === 'all'
              ? 'bg-[#183a6b] text-white shadow-2xs'
              : 'border border-slate-200 bg-white text-slate-800 hover:bg-slate-50'
          }`}
        >
          All 5
        </button>

        {/* Button 2: Needs you 2 */}
        <button
          type="button"
          onClick={() => handleFilterClick('needs-you')}
          className={`min-h-[34px] rounded-full px-4 text-xs font-semibold transition-all ${
            activeFilter === 'needs-you'
              ? 'bg-[#183a6b] text-white shadow-2xs'
              : 'border border-slate-200 bg-white text-slate-800 hover:bg-slate-50'
          }`}
        >
          Needs you 2
        </button>

        {/* Button 3: Auto-handled 1 */}
        <button
          type="button"
          onClick={() => handleFilterClick('auto-handled')}
          className={`min-h-[34px] rounded-full px-4 text-xs font-semibold transition-all ${
            activeFilter === 'auto-handled'
              ? 'bg-[#183a6b] text-white shadow-2xs'
              : 'border border-slate-200 bg-white text-slate-800 hover:bg-slate-50'
          }`}
        >
          Auto-handled 1
        </button>
      </div>

      {/* 2. Data Table */}
      <div className="overflow-hidden rounded-xl border border-slate-200/90 bg-white shadow-2xs">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left">
            {/* Table Header */}
            <thead>
              <tr className="border-b border-slate-200/90 bg-slate-50/80">
                <th
                  scope="col"
                  className="w-[120px] px-6 py-3.5 text-[11px] font-bold tracking-wider text-slate-500 uppercase"
                >
                  ID
                </th>
                <th
                  scope="col"
                  className="w-[200px] px-6 py-3.5 text-[11px] font-bold tracking-wider text-slate-500 uppercase"
                >
                  FROM
                </th>
                <th
                  scope="col"
                  className="px-6 py-3.5 text-[11px] font-bold tracking-wider text-slate-500 uppercase"
                >
                  WHAT
                </th>
                <th
                  scope="col"
                  className="w-[120px] px-6 py-3.5 text-[11px] font-bold tracking-wider text-slate-500 uppercase"
                >
                  RECEIVED
                </th>
                <th
                  scope="col"
                  className="w-[160px] px-6 py-3.5 text-[11px] font-bold tracking-wider text-slate-500 uppercase"
                >
                  STATUS
                </th>
              </tr>
            </thead>

            {/* Table Body */}
            <tbody className="divide-y divide-slate-100">
              {filteredIssues.map((issue) => (
                <tr
                  key={issue.id}
                  onClick={() => onIssueSelect?.(issue)}
                  className="cursor-pointer transition-colors hover:bg-slate-50/80"
                >
                  {/* ID */}
                  <td className="px-6 py-4 text-xs font-bold text-slate-900 whitespace-nowrap">
                    {issue.id}
                  </td>

                  {/* FROM */}
                  <td className="px-6 py-4 text-xs text-slate-600 whitespace-nowrap">
                    {issue.from}
                  </td>

                  {/* WHAT */}
                  <td className="px-6 py-4 text-xs text-slate-600">
                    {issue.what}
                  </td>

                  {/* RECEIVED */}
                  <td className="px-6 py-4 text-xs text-slate-600 whitespace-nowrap">
                    {issue.received}
                  </td>

                  {/* STATUS */}
                  <td className="px-6 py-4 text-xs whitespace-nowrap">
                    {issue.status === 'Needs reply' && (
                      <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-800">
                        <svg
                          className="h-3.5 w-3.5 text-amber-600"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2.5"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        >
                          <path d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
                        </svg>
                        Needs reply
                      </span>
                    )}

                    {issue.status === 'Decision needed' && (
                      <span className="inline-flex items-center gap-1.5 rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700">
                        <svg
                          className="h-3.5 w-3.5 text-rose-600"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2.5"
                        >
                          <circle cx="12" cy="12" r="10" />
                          <line x1="15" y1="9" x2="9" y2="15" />
                          <line x1="9" y1="9" x2="15" y2="15" />
                        </svg>
                        Decision needed
                      </span>
                    )}

                    {issue.status === 'Auto-handled' && (
                      <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">
                        <svg
                          className="h-3.5 w-3.5 text-emerald-600"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2.5"
                        >
                          <circle cx="12" cy="12" r="9" />
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="m9 12 2 2 4-4"
                          />
                        </svg>
                        Auto-handled
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* 3. Footer Note */}
      <div className="flex items-center gap-2 pt-1 text-xs text-slate-500">
        <svg
          className="h-4 w-4 shrink-0 text-slate-400"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="16" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12.01" y2="8" />
        </svg>
        <span>
          Shortfalls, partial deliveries and remainder orders are handled automatically - they appear here for the record only.
        </span>
      </div>
    </div>
  );
};

export default IssuesInbox;
