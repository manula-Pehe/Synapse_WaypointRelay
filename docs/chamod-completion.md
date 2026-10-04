# Chamod scope completion

This document tracks the work after the supplied design guide update. F1 and F10 are intentionally excluded at the user's request. The previously tracked `docs/dispatch-frontend-integration.md` was already deleted in the worktree when this work began; that deletion is left untouched.

## F3 — order queue

- The D1u unconfirmed list now offers a `tel:` call action when the backend supplies a phone number. The existing unconfirmed API explicitly returns `null` today because outlets do not yet store phone numbers; the UI shows “Not available” and does not invent a number.
- D1b accepts multiple ambient/chilled lines. It sends each line through the existing phone-in endpoint, retains unsubmitted lines if a request fails, reports how many were created, and refreshes the queue. The endpoint creates one order per line, so a multi-line submission is not atomic.
- Verified with frontend build and lint.
