import React, { useState } from 'react';

export interface IssueDetailProps {
  issueId?: string;
  onBack?: () => void;
  onSendReplyAndResolve?: (replyText: string, actionType: string) => void;
  onSaveReplyOnly?: (replyText: string) => void;
  onDepotChange?: (depot: string) => void;
  onNotificationClick?: () => void;
}

export const IssueDetail: React.FC<IssueDetailProps> = ({
  issueId = 'ISS-0142',
  onBack,
  onSendReplyAndResolve,
  onSaveReplyOnly,
  onDepotChange,
  onNotificationClick,
}) => {
  const [selectedDepot, setSelectedDepot] = useState('Peliyagoda');
  const [selectedAction, setSelectedAction] = useState<'add-case' | 'record-only' | 'ask-info'>('add-case');
  const [replyText, setReplyText] = useState(
    "Sorry about that. 1 replacement case is added to your Fri 2 Oct chilled order (18 -> 19 cases). I've asked the loaders to stack yoghurt on top."
  );
  const [isResolved, setIsResolved] = useState(false);

  const handleDepotSelect = (depot: string) => {
    setSelectedDepot(depot);
    onDepotChange?.(depot);
  };

  const handleSendAndResolve = () => {
    setIsResolved(true);
    onSendReplyAndResolve?.(replyText, selectedAction);
  };

  const handleSaveOnly = () => {
    onSaveReplyOnly?.(replyText);
  };

  return (
    <div className="space-y-6 font-sans">
      {/* 1. Top Header Section */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200/80 pb-4">
        {/* Title & Subtitle */}
        <div className="flex items-center gap-3">
          {onBack && (
            <button
              type="button"
              onClick={onBack}
              className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 transition hover:bg-slate-50"
              title="Back to Issues"
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </button>
          )}
          <div>
            <h1 className="text-xl font-bold tracking-tight text-slate-900 sm:text-2xl">
              {issueId} · Damaged · 1 case
            </h1>
            <p className="mt-0.5 text-xs text-slate-500 sm:text-sm">
              OUT001 Colombo · order S1-001 · reported 7:44 AM by Dilani J.
            </p>
          </div>
        </div>

        {/* Right side controls */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Depot toggle buttons */}
          <div className="flex min-h-[38px] items-center rounded-lg bg-slate-100 p-1">
            {['Peliyagoda', 'Kandy', 'All depots'].map((depot) => {
              const isSelected = selectedDepot === depot;
              return (
                <button
                  key={depot}
                  type="button"
                  onClick={() => handleDepotSelect(depot)}
                  className={`min-h-[30px] rounded-md px-3 text-xs font-medium transition-all ${
                    isSelected
                      ? 'bg-white text-slate-900 shadow-2xs font-semibold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {depot}
                </button>
              );
            })}
          </div>

          {/* Date pill */}
          <div className="flex min-h-[38px] items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-2xs">
            <svg
              className="h-4 w-4 text-slate-500"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            <span>Run: Thu 1 Oct</span>
          </div>

          {/* Status Badge: Needs reply / Resolved */}
          <div
            className={`flex min-h-[38px] items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold ${
              isResolved
                ? 'border border-emerald-200/90 bg-emerald-50/80 text-emerald-800'
                : 'border border-amber-200/90 bg-amber-50/80 text-amber-800'
            }`}
          >
            {isResolved ? (
              <svg className="h-3.5 w-3.5 text-emerald-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="12" cy="12" r="9" />
                <path strokeLinecap="round" strokeLinejoin="round" d="m9 12 2 2 4-4" />
              </svg>
            ) : (
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
            )}
            <span>{isResolved ? 'Resolved' : 'Needs reply'}</span>
          </div>

          {/* Notification Bell */}
          <button
            type="button"
            aria-label="Notifications"
            onClick={onNotificationClick}
            className="flex min-h-[38px] min-w-[38px] items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 shadow-2xs transition hover:bg-slate-50 hover:text-slate-900"
          >
            <svg
              className="h-4 w-4"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" />
            </svg>
          </button>
        </div>
      </div>

      {/* 2. Main Two-Column Layout */}
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
        {/* Left Column (~65% width) */}
        <div className="space-y-6 lg:col-span-8">
          {/* Section 1: From the store */}
          <div className="rounded-2xl border border-slate-200/90 bg-white p-6 shadow-2xs">
            <h2 className="text-sm font-bold text-slate-900 sm:text-base">
              From the store
            </h2>

            {/* Message Bubble */}
            <div className="mt-4 rounded-xl bg-slate-100/90 p-4">
              <div className="text-xs font-bold text-slate-700">
                Dilani J. · 7:44 AM
              </div>
              <p className="mt-1 text-xs text-slate-800 leading-relaxed sm:text-sm">
                1 yoghurt case crushed at the bottom of the stack, 6 cups leaking. Please replace on the next delivery.
              </p>
            </div>

            {/* Attachment Placeholder */}
            <div className="mt-4 flex h-32 w-32 items-center justify-center rounded-xl bg-slate-200/70 text-slate-500 shadow-inner">
              <svg
                className="h-8 w-8 text-slate-500"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z" />
                <circle cx="12" cy="13" r="3" />
              </svg>
            </div>
          </div>

          {/* Section 2: Resolve */}
          <div className="rounded-2xl border border-slate-200/90 bg-white p-6 shadow-2xs">
            <h2 className="text-sm font-bold text-slate-900 sm:text-base">
              Resolve
            </h2>

            {/* Action Buttons Row */}
            <div className="mt-4 flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => setSelectedAction('add-case')}
                className={`min-h-[34px] rounded-full px-4 text-xs font-semibold transition-all ${
                  selectedAction === 'add-case'
                    ? 'bg-[#183a6b] text-white shadow-2xs'
                    : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                }`}
              >
                Add 1 case to next order
              </button>

              <button
                type="button"
                onClick={() => setSelectedAction('record-only')}
                className={`min-h-[34px] rounded-full px-4 text-xs font-semibold transition-all ${
                  selectedAction === 'record-only'
                    ? 'bg-[#183a6b] text-white shadow-2xs'
                    : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                }`}
              >
                Record only
              </button>

              <button
                type="button"
                onClick={() => setSelectedAction('ask-info')}
                className={`min-h-[34px] rounded-full px-4 text-xs font-semibold transition-all ${
                  selectedAction === 'ask-info'
                    ? 'bg-[#183a6b] text-white shadow-2xs'
                    : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                }`}
              >
                Ask for more info
              </button>
            </div>

            {/* Text Area Label */}
            <label
              htmlFor="reply-to-store"
              className="mt-4 block text-xs font-bold text-slate-900"
            >
              Reply to store
            </label>

            {/* Text Area Box */}
            <div className="mt-2">
              <textarea
                id="reply-to-store"
                rows={3}
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                className="w-full rounded-xl border border-slate-200 p-3.5 text-xs text-slate-800 leading-relaxed transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none sm:text-sm"
              />
            </div>

            {/* Footer Buttons */}
            <div className="mt-4 flex flex-wrap items-center gap-3">
              {/* Primary: Send reply and resolve */}
              <button
                type="button"
                onClick={handleSendAndResolve}
                className="inline-flex min-h-[38px] items-center gap-2 rounded-xl bg-[#183a6b] px-4 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-[#122e54] active:scale-[0.99]"
              >
                <svg
                  className="h-3.5 w-3.5 -rotate-45"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <line x1="22" y1="2" x2="11" y2="13" />
                  <polygon points="22 2 15 22 11 13 2 9 22 2" />
                </svg>
                <span>Send reply and resolve</span>
              </button>

              {/* Secondary: Save reply only */}
              <button
                type="button"
                onClick={handleSaveOnly}
                className="inline-flex min-h-[38px] items-center rounded-xl border border-[#183a6b] bg-white px-4 py-2 text-xs font-bold text-[#183a6b] transition hover:bg-blue-50/50 active:scale-[0.99]"
              >
                Save reply only
              </button>
            </div>
          </div>
        </div>

        {/* Right Column (~35% width) - Section 3: Linked delivery */}
        <div className="rounded-2xl border border-slate-200/90 bg-white p-6 shadow-2xs lg:col-span-4">
          <h2 className="text-sm font-bold text-slate-900 sm:text-base">
            Linked delivery
          </h2>

          <div className="mt-4 divide-y divide-slate-100">
            {/* Row 1: Order */}
            <div className="flex items-center justify-between py-2.5 text-xs sm:text-sm">
              <span className="text-slate-500">Order</span>
              <span className="font-semibold text-slate-900">
                S1-001 · chilled · 70 cases
              </span>
            </div>

            {/* Row 2: Delivered */}
            <div className="flex items-center justify-between py-2.5 text-xs sm:text-sm">
              <span className="text-slate-500">Delivered</span>
              <span className="font-semibold text-slate-900">
                78 of 80 · 7:14 AM
              </span>
            </div>

            {/* Row 3: Driver noted */}
            <div className="flex items-center justify-between py-2.5 text-xs sm:text-sm">
              <span className="text-slate-500">Driver noted</span>
              <span className="font-semibold text-slate-900">
                2 cases damaged in transit
              </span>
            </div>

            {/* Row 4: Remainder */}
            <div className="flex items-center justify-between py-2.5 text-xs sm:text-sm">
              <span className="text-slate-500">Remainder</span>
              <span className="font-semibold text-slate-900">
                S1-001-R · 2 cases · Fri 2 Oct
              </span>
            </div>

            {/* Row 5: Next order */}
            <div className="flex items-center justify-between py-2.5 text-xs sm:text-sm">
              <span className="text-slate-500">Next order</span>
              <span className="font-semibold text-slate-900">
                ORD-1002-001C · Fri 2 Oct · 18 cases
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default IssueDetail;
