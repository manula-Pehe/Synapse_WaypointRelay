# Chamod scope completion

This document tracks the work after the supplied design guide update. F1 and F10 are intentionally excluded at the user's request. The previously tracked `docs/dispatch-frontend-integration.md` was already deleted in the worktree when this work began; that deletion is left untouched.

## F3 — order queue

- The D1u unconfirmed list now offers a `tel:` call action when the backend supplies a phone number. The existing unconfirmed API explicitly returns `null` today because outlets do not yet store phone numbers; the UI shows “Not available” and does not invent a number.
- D1b accepts multiple ambient/chilled lines. It sends each line through the existing phone-in endpoint, retains unsubmitted lines if a request fails, reports how many were created, and refreshes the queue. The endpoint creates one order per line, so a multi-line submission is not atomic.
- Verified with frontend build and lint.

## F5 — live board

- Added `GET /api/dispatch/live?runDate=&depot=` under dispatcher authorization and depot scope. The response aggregates the published plan, current order states, outcome timestamps, deferrals needing a decision, and planned late risks. Critical failed deliveries sort ahead of warnings.
- The responsive desktop/phone board polls every 15 seconds and opens an order detail/history drawer from an actionable alert. The old hard-coded desktop and phone sample boards were removed.
- On-time percentage uses recorded delivery outcome events compared with the planned stop window; it is `null` until an outcome is recorded. Fridge use is based on the published plan summary. Missing driver sync, skipped-run history, vehicle problems, and dock shortfalls are shown as unavailable or omitted because their source workflows are not present. No guessed progress or GPS position is shown.
- A focused backend aggregation test covers on-time arithmetic, trip progress, fridge use, and critical-first ordering. Backend compilation and the focused test pass with annotation processing disabled (the installed Lombok processor is incompatible with the available JDK 27; no source uses Lombok annotations). Frontend build and lint pass.
