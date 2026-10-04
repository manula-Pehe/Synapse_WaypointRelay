# Waypoint Relay frontend

## Run locally

```sh
npm install
cp .env.example .env.local
npm run dev
```

Sign-in uses `POST /api/auth/login`. There is no mock sign-in or automatic fallback.
`VITE_API_TARGET` selects the backend forwarded through the local `/api` proxy.
Store manager screens always load their operational data from backend endpoints.
Provision accounts before signing in. Demo credentials are not included in the frontend.

## Extend the shell

- Add role screens as child routes under their layout in `src/App.tsx`.
- Import `useAuth` from `src/app/auth` for `user`, `token`, `login`, `logout`, and language.
- Use `api<T>('store/orders')` from `src/lib/api` inside TanStack Query functions.
  The client adds the bearer token and throws `ApiError` with `status`, `code`, and
  `message`. Authenticated 401s clear the session; 403s report “No access”.
- Sign-out clears cached server data. The “Keep me signed in” checkbox chooses local storage (checked) or tab session storage (unchecked). The selected language persists locally.
  Language is currently a browser preference, stored alongside the session's user.
- Centralized EN/SI/TA strings are in `src/i18n/messages.ts`. No extra i18n dependency
  has been introduced; migrate to the shared react-i18next setup when it is added.
- Guards improve navigation; the backend must still enforce role and outlet access.

## Checks

```sh
npm run lint
npm run build
npm test
```

For a manual check, sign in with each account, refresh, visit another role's route,
and sign out. Wrong-role URLs should return to the user's own workspace; signed-out
URLs should return to `/login`. Check the sign-in page at 390px and desktop widths.

## Changes from the Designathon design

X1 desktop styling follows the supplied screenshot, with a stacked mobile layout.
The Inter font is bundled locally under the SIL Open Font License. The password
reset and offline-driver notes reproduce design copy; those workflows are outside
this shell. Dedicated loader/driver sign-in screens belong to their respective
features. Role home pages remain placeholders.

## Notifications (F3)

Every role header includes a shared bell linking to its `/notifications` child route.
Notifications poll every 30 seconds. The S11 store view follows the supplied desktop
reference with a sidebar, category filters, day sections, timestamps and unread dots.
S11m uses stacked cards and fixed bottom navigation. D14 is a native modal side panel
with severity filters, keyboard focus containment, and Escape/close dismissal. X2 uses a responsive fallback until its
reference is supplied. English, Sinhala and Tamil copy is included.

Opening a card marks it read and shows its details; mark-all-read also persists per
user on the backend. Every role reads from `GET /api/notifications` and sends reads to
`POST /api/notifications/{id}/read`. Store categories and needs-action filtering are
independent of read state. The backend currently sends English title and body text.
The dispatch panel overlays the existing shell; the live board belongs to the dispatch feature.

The phone retains accessible filters and mark-all-read controls in addition to the
reference layout. Controls have at least 48px targets; the OS status bar is not part
of the web page. See `docs/store-production-readiness.md` for remaining backend work.
