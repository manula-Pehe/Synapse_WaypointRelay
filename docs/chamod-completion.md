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

## F8 — run report

- Added `GET /api/dispatch/reports/run?runDate=&depot=`. It reports the published plan's deferrals, recorded failed and partial order outcomes, on-time results from the live aggregation, late outcomes by district from planned stop windows, and recorded exceptions.
- Replaced the sample report with a run/depot-aware screen and CSV export of the returned data. The old sample date filters and example values were removed.
- A failed outcome cannot count as on-time, even if recorded before the window closes. Runs without a published plan show unknown plan-derived metrics as `null`, and district totals count only stops with recorded outcomes.
- Verified with the focused report and live aggregation backend tests, frontend build, and lint.

## F9 — network map

- The district schematic now consumes the same 15-second live trip response as F5. District trip totals, completed stops, and Planned / On the way / Completed counts use recorded trip progress from the published plan and order states.
- A missing or draft plan has its own empty state. The SVG shows depot-to-district connections only; there are no location coordinates or GPS dots.
- Updated the district aggregation test and verified the frontend build, lint, and test suite.

## Final review

- Failed outcomes remain in the report's failed count and exception list; the district late-delivery denominator counts delivered and partial outcomes only.
- SVG trip counts and the depot label use the theme's on-brand text token in dark mode.
- F5 intentionally uses only data recorded by the current backend, per the user's scope decision. Driver sync, vehicle problems, sync conflicts, dock shortfalls, and two-run skip history require their separate source workflows before they can appear as live facts.

## Develop merge

- Fetched `origin/develop` at `fe98324` and merged it into the dispatch branch. The merge brought in store ordering, delivery, issue, receipt, and planning changes.
- Resolved `frontend/src/App.tsx` by retaining the API-backed dispatcher workspace while adding the new store routes from develop. Resolved the sidebar conflict by passing develop's sign-out callback through the existing account menu, which continues to use the signed-in user's real profile and language settings.
- The local, uncommitted deletion of `docs/dispatch-frontend-integration.md` existed before the merge and remains outside the merge commit.
- Verified with frontend build, lint, six tests, and the focused dispatcher backend tests.
