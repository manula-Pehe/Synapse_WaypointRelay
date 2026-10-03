# Waypoint Relay frontend

## Run locally

```sh
npm install
cp .env.example .env.local
npm run dev
```

`.env.local` opts into mock login. Set `VITE_MOCK_AUTH=false` and restart Vite to use
`POST /api/auth/login`. There is no automatic fallback to mocks on API failures.
`VITE_API_TARGET` selects the backend forwarded through the local `/api` proxy.
Production defaults to real login unless mock mode is explicitly enabled at build time.
Store manager screens always load their operational data from backend endpoints.

| Role | Identifier | Password / PIN |
| --- | --- | --- |
| Store manager | dilani@waypoint.lk | Relay@2026 |
| Dispatcher | ruwan@waypoint.lk | Relay@2026 |
| Loader | PELIYAGODA | 1234 |
| Driver | DRV-0036 | 3636 |

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
with severity filters, keyboard focus containment, Escape/close dismissal, and 3/2/3
critical/warning/info sample notifications. X2 uses a responsive fallback until its
reference is supplied. English, Sinhala and Tamil copy is included.

Opening a card marks it read and shows its details; mark-all-read also persists per
user and role. Store categories and needs-action filtering are independent of read
state. Demo fixtures use fictional quantities and IDs; loader/driver have empty lists
until their fixtures or API are available. Notifications remain demo data regardless
of authentication mode. Replace `features/notifications/data.ts` when the backend
contract arrives. Receipt and issue actions are not implemented here; details explain
that limitation, and unfinished navigation destinations are disabled. The dispatch
panel overlays the existing shell; the live board belongs to the dispatch feature.

The phone retains accessible filters and mark-all-read controls in addition to the
reference layout. Controls have at least 48px targets; the OS status bar is not part
of the web page. Live API integration remains outstanding.
