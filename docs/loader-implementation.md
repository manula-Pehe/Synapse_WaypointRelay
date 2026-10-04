# Loader implementation log

Branch: `codex/loader-vihanj`. Scope: F1–F6 of the VihanJ loader brief. The brief and exported PNGs are design references; behavior below reflects code in this branch.

## F1 · Shared dock sign-in

- Added `/loader/sign-in` with a four-digit PIN keypad and failure state. It calls the existing auth endpoint with `identifier=PELIYAGODA`, keeps the token in session storage, and clears a wrong PIN.
- Added a loader shell with the signed-in loader, switch action, dock context, demo-clock time, and connectivity indicator. The indicator says **Online** only when the settings request succeeds; it does not claim data has synced offline.
- Allowed unauthenticated `GET /api/settings` so the sign-in header can show the server demo clock. The clock-changing POST remains dispatcher-only.
- Unauthenticated loader routes redirect to the dock sign-in screen.

## F2–F6 · In progress

The loader migration, APIs, and UI flows are being completed and verified in subsequent commits.

## Known design/data gaps

- The existing order and outlet read models expose dock and parking details, but no store-specific handling note. The loader stop detail shows recorded access information only.
- The Figma shortfall dialog includes a photo action. The F1–F6 API contract has no loader photo upload endpoint, so this action is not presented as functional.
- Handover changes the trip to `LOADED`. Departure is shown only after a driver updates the orders to `ON_THE_WAY`; the loader does not claim a truck has left at handover time.
